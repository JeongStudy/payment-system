package com.system.payment.payment.service;

import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerConflictException;
import com.system.payment.exception.PaymentServerNotFoundException;
import com.system.payment.payment.domain.*;
import com.system.payment.payment.model.dto.PaymentDetailItem;
import com.system.payment.payment.model.request.CreatePaymentRequest;
import com.system.payment.payment.model.response.CreatePaymentResponse;
import com.system.payment.payment.model.response.IdempotencyKeyResponse;
import com.system.payment.payment.model.response.PaymentStatusResponse;
import com.system.payment.payment.repository.PaymentHistoryRepository;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.user.domain.AesKey;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.service.CredentialService;
import com.system.payment.user.service.CryptoService;
import com.system.payment.user.service.UserService;
import com.system.payment.util.KeyGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentRequestService {

	private final UserService userService;
	private final CryptoService cryptoService;
	private final CredentialService credentialService;
	private final PaymentHistoryService paymentHistoryService;
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
	 * <p>
	 * 추가는 TODO에 있음.
	 */
	@Transactional
	public CreatePaymentResponse createPaymentAndPublish(CreatePaymentRequest request) {

		if (paymentRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
			throw new PaymentServerConflictException(ErrorCode.DUPLICATE_PAYMENT_IDEMPOTENCY_KEY);
		}

		final PaymentUser paymentUser = userService.findUser();

		final AesKey aesKey = cryptoService.resolveValidAesKey(request.getRsaPublicKey(), request.getEncAesKey());
		final String decryptedPassword = cryptoService.decryptPasswordWithAes(request.getEncPassword(), aesKey.getAesKey());
		credentialService.verifyOrThrow(decryptedPassword, paymentUser.getPassword());

		List<PaymentDetailItem> itemList = new ArrayList<>();
		itemList.add(PaymentDetailItem.product(1, 1));
//		itemList.add(PaymentDetailItem.point(2, -5000));
		PaymentItemValidator.validateAndVerifyTotal(itemList, request.getAmount());

		final PaymentUserCard paymentUserCard = paymentUserCardRepository.findById(request.getPaymentUserCardId())
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.NOT_FOUND));

		String transactionId = KeyGeneratorUtil.generateTransactionId();

		final Payment payment = paymentRepository.save(
				Payment.create(
						PaymentUserRef.of(paymentUser.getId()),
						ReferenceRef.of(ReferenceType.ORDER, request.getServiceOrderId()),
						PaymentMethodRef.of(PaymentMethodType.CARD, paymentUserCard.getId()),
						PaymentType.NORMAL,
						request.getAmount(),
						request.getIdempotencyKey(),
						transactionId,
						itemList
				));

		paymentHistoryService.recordCreated(payment);

		registerAfterCommit(() -> paymentProducer.sendPaymentRequested(payment, paymentUser, paymentUserCard, request));

		payment.changeResultCodeRequested();

		return CreatePaymentResponse.from(payment);
	}

	private void registerAfterCommit(Runnable task) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				task.run();
			}
		});
	}
}