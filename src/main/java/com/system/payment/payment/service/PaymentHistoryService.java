package com.system.payment.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.common.dto.response.ErrorCode;
import com.system.payment.common.exception.PaymentServerInternalServerErrorException;
import com.system.payment.payment.domain.entity.Payment;
import com.system.payment.payment.domain.entity.PaymentHistory;
import com.system.payment.payment.domain.constant.PaymentResultCode;
import com.system.payment.payment.repository.PaymentHistoryRepository;
import com.system.payment.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

	private final PaymentHistoryRepository paymentHistoryRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public void recordCreated(Payment payment) {
		String paymentSnapshot = null;
		try {
			paymentSnapshot = objectMapper.writeValueAsString(payment);
		} catch (JsonProcessingException e) {
			throw new PaymentServerInternalServerErrorException(ErrorCode.SERVER_ERROR);
		}
		paymentHistoryRepository.save(PaymentHistory.
				create(payment, payment.getPaymentResultCode().getCode(), payment.getCreatedTimestamp(), "SYSTEM",
						"create payment", paymentSnapshot, payment.getTransactionId()));
	}

	// TODO: recordCanceled

	@Transactional
	public void recordRequested(Payment payment, PaymentResultCode prevCode,
								String prevDataJson, String txId,
								String changedBy, String reason, Object externalResponse) {
		recordTransition(payment, prevCode, prevDataJson, txId, changedBy, reason, externalResponse);
	}

	@Transactional
	public void recordCompleted(Payment payment, PaymentResultCode prevCode,
								String prevDataJson, String txId,
								String changedBy, String reason, Object externalResponse) {
		recordTransition(payment, prevCode, prevDataJson, txId, changedBy, reason, externalResponse);
	}

	@Transactional
	public void recordFailed(Payment payment, PaymentResultCode prevCode,
							 String prevDataJson, String txId,
							 String changedBy, String reason, Object externalResponse) {
		recordTransition(payment, prevCode, prevDataJson, txId, changedBy, reason, externalResponse);
	}

	// 공통 구현부
	private void recordTransition(Payment payment, PaymentResultCode prevCode,
								  String prevDataJson, String txId,
								  String changedBy, String reason, Object externalResponse) {
//		String newDataJson = StringUtils.toJsonSafe(payment);
//		String externalJson = StringUtils.toJsonSafe(externalResponse);

		String newDataJson = Optional.ofNullable(StringUtils.toJsonSafe(payment)).orElse("{}");
		String externalJson = Optional.ofNullable(StringUtils.toJsonSafe(externalResponse)).orElse("{}");

		paymentHistoryRepository.save(
				PaymentHistory.createFull(
						payment,
						prevCode != null ? prevCode.getCode() : null,
						payment.getPaymentResultCode().getCode(),
						LocalDateTime.now(),
						changedBy,
						reason,
						prevDataJson,
						newDataJson,
						externalJson,
						txId
				)
		);
	}
}
