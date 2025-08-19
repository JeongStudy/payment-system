package com.system.payment.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerInternalServerErrorException;
import com.system.payment.payment.domain.Payment;
import com.system.payment.payment.domain.PaymentHistory;
import com.system.payment.payment.repository.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	// 이후 확장: recordRequested, recordCompleted, recordFailed, recordCanceled ...

}
