package com.system.payment.payment.service;

import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerNotFoundException;
import com.system.payment.payment.domain.*;
import com.system.payment.payment.model.request.CreatePaymentRequest;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.response.CreatePaymentResponse;
import com.system.payment.payment.model.response.IdempotencyKeyResponse;
import com.system.payment.payment.repository.PaymentHistoryRepository;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.user.domain.AesKey;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.service.CredentialService;
import com.system.payment.user.service.CryptoService;
import com.system.payment.user.service.UserService;
import com.system.payment.util.TransactionIdUtil;
import com.system.payment.util.KeyGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class PaymentRequestService {

	private final UserService userService;
	private final CryptoService cryptoService;
	private final CredentialService credentialService;
	private final PaymentUserCardRepository paymentUserCardRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentHistoryRepository paymentHistoryRepository;
	private final PaymentProducer paymentProducer;

	@Transactional
	public IdempotencyKeyResponse getIdempotencyKey() {
		String key = KeyGeneratorUtil.generateIdempotencyKey();
		return IdempotencyKeyResponse.from(key);
	}

	/**
	 * 프로세스:
	 * 1) 비밀번호 검증
	 * 2) (생략) 주문 검증
	 * 3) 사용자 카드 조회
	 * 4) Payment/Detail/History 생성 (상태=대기 "00")
	 * 5) 상태 변경 정책: 프로듀서는 "대기"까지만; "요청(11)"은 컨슈머가 수신 즉시 변경
	 * 6) 커밋 후 결제요청 Kafka 전송
	 * 7) (컨슈머에서) Payment 상태값 "요청(11)"로 변경
	 */
	@Transactional
	public CreatePaymentResponse createAndPublish(CreatePaymentRequest request) {
		// 유저 조회
		final PaymentUser paymentUser = userService.findUser();

		// 암 복호화
		final AesKey aesKey = cryptoService.resolveValidAesKey(request.getRsaPublicKey(), request.getEncAesKey());
		final String decryptedPassword = cryptoService.decryptPasswordWithAes(request.getEncPassword(), aesKey.getAesKey());

		// 비밀번호 검증
		credentialService.verifyOrThrow(decryptedPassword, paymentUser.getPassword());

		// 2) 주문번호 검증 - 생략 (추후 ACL로 교체)

		// 3) 사용자 카드 조회(빌링키)
		final PaymentUserCard paymentUserCard = paymentUserCardRepository.findById(request.getPaymentUserCardId())
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.NOT_FOUND));

		// 트랜잭션 id 생성
		String transactionId = TransactionIdUtil.generate();

		// 4) Payment/Detail/History 생성 (상태=대기 "00")
		Payment payment = Payment.create(
				PaymentUserRef.of(paymentUser.getId()),
				ReferenceRef.of(ReferenceType.ORDER, request.getServiceOrderId()),
				PaymentMethodRef.of(PaymentMethodType.CARD, paymentUserCard.getId()),
				PaymentType.NORMAL,
				request.getAmount(),
				request.getIdempotencyKey(),
				transactionId
		);

		// 예시
		Integer ItemId = 1;

		// 단일 수단이면 전체 금액으로 1개의 detail 생성 (여러 수단/부분결제면 반복 생성)
		payment.addDetail(ItemId, ItemType.PRODUCT, payment.getTotalAmount());

		payment = paymentRepository.save(payment);

		// History: prev=null -> new="00"
		PaymentHistory history = PaymentHistory.builder()
				.payment(payment)
				.newResultCode(PaymentResultCode.WAITING.getCode())
				.changedAt(payment.getCreatedTimestamp())
				.changedBy("SYSTEM")
				.changedReason("create payment")
				.newData(snapshot(payment))
				.transactionId(transactionId)
				.build();

		paymentHistoryRepository.save(history);


		// 6) 커밋 후 결제요청 이벤트 전송 (미전달 시 상태 "00" 유지 → 모니터링 가능)
		InicisBillingApproval inicisBillingApproval = InicisBillingApproval.builder()
				.build();


		registerAfterCommit(() -> paymentProducer.sendPaymentRequested(paymentUser, inicisBillingApproval));


		// 상태변경


		// 응답
		return CreatePaymentResponse.builder()
				.serviceOrderId(request.getServiceOrderId())
				.paymentId(payment.getId())
				.build();
	}

	private void registerAfterCommit(Runnable task) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				task.run();
			}
		});
	}

	private String snapshot(Payment p) {
		return "{\"paymentId\":" + p.getId()
				+ ",\"code\":\"" + p.getPaymentResultCode().getCode()
				+ "\",\"amount\":" + p.getTotalAmount() + "}";
	}
}