package com.system.payment.payment.service;

import com.system.payment.card.domain.entity.PaymentUserCard;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.common.dto.response.ErrorCode;
import com.system.payment.common.exception.PaymentServerConflictException;
import com.system.payment.common.exception.PaymentServerNotFoundException;
import com.system.payment.outbox.service.OutboxService;
import com.system.payment.payment.domain.constant.PaymentMethodType;
import com.system.payment.payment.domain.constant.PaymentType;
import com.system.payment.payment.domain.constant.ReferenceType;
import com.system.payment.payment.domain.entity.Payment;
import com.system.payment.payment.domain.vo.PaymentMethodRef;
import com.system.payment.payment.domain.vo.PaymentUserRef;
import com.system.payment.payment.domain.vo.ReferenceRef;
import com.system.payment.payment.model.dto.PaymentDetailItem;
import com.system.payment.payment.model.request.CreatePaymentRequest;
import com.system.payment.payment.model.response.CreatePaymentResponse;
import com.system.payment.payment.model.response.IdempotencyKeyResponse;
import com.system.payment.payment.model.response.PaymentStatusResponse;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.user.domain.AesKey;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.service.CredentialService;
import com.system.payment.user.service.CryptoService;
import com.system.payment.user.service.UserService;
import com.system.payment.common.util.KeyGeneratorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentRequestService {

	private final UserService userService;
	private final CryptoService cryptoService;
	private final CredentialService credentialService;
	private final PaymentHistoryService paymentHistoryService;
	private final OutboxService outboxService;
	private final PaymentUserCardRepository paymentUserCardRepository;
	private final PaymentRepository paymentRepository;

	@Transactional
	public IdempotencyKeyResponse getIdempotencyKey() {
		String key = KeyGeneratorUtils.generateIdempotencyKey();
		return IdempotencyKeyResponse.from(key);
	}

	@Transactional
	public CreatePaymentResponse createPaymentAndPublish(CreatePaymentRequest request) {
		final String productName = request.getProductName();

		if (paymentRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
			throw new PaymentServerConflictException(ErrorCode.DUPLICATE_PAYMENT_IDEMPOTENCY_KEY);
		}

		final PaymentUser paymentUser = userService.findUser();

		final AesKey aesKey = cryptoService.resolveValidAesKey(request.getRsaPublicKey(), request.getEncAesKey());
		final String decryptedPassword = cryptoService.decryptPasswordWithAes(request.getEncPassword(), aesKey.getAesKey());
		credentialService.verifyOrThrow(decryptedPassword, paymentUser.getEncPassword());

		List<PaymentDetailItem> itemList = new ArrayList<>();
		itemList.add(PaymentDetailItem.order(1, 1));
//		itemList.add(PaymentDetailItem.point(2, -5000));

		final PaymentUserCard paymentUserCard = paymentUserCardRepository.findById(request.getPaymentUserCardId())
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.NOT_FOUND));

		String transactionId = KeyGeneratorUtils.generateTransactionId();

		final Payment payment = Payment.create(
				PaymentUserRef.of(paymentUser.getId()),
				ReferenceRef.of(ReferenceType.ORDER,
						request.getServiceOrderId()),
				PaymentMethodRef.of(PaymentMethodType.CARD,
						paymentUserCard.getId()),
				PaymentType.NORMAL,
				request.getAmount(),
				request.getIdempotencyKey(),
				transactionId,
				itemList
		);
		paymentRepository.save(payment);


		paymentHistoryService.recordCreated(payment);

		Integer eventId = outboxService.enqueuePaymentRequested(
				payment.getId(),
				payment.getTransactionId(),
				payment.getUserRef().getUserId(),
				payment.getMethodRef().getPaymentMethodType().name(),
				payment.getMethodRef().getPaymentMethodId(),
				productName
		);

		return CreatePaymentResponse.from(payment, eventId);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public PaymentStatusResponse getPaymentStatus(Integer paymentId) {
		var payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.NOT_FOUND));

		return PaymentStatusResponse.builder()
				.paymentId(payment.getId())
				.code(payment.getPaymentResultCode().getCode())
				.description(payment.getPaymentResultCode().getDescription())
				.build();
	}
}