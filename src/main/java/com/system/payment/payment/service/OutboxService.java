package com.system.payment.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.payment.domain.outbox.OutboxEvent;
import com.system.payment.payment.domain.outbox.PaymentRequestedArgs;
import com.system.payment.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxService {
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Transactional
	public Integer enqueuePaymentRequested(Integer paymentId, String txId, Integer userId, String methodType, Integer methodId, String productName) {
		try {
			var args = new PaymentRequestedArgs(paymentId, txId, userId, methodType, methodId, productName);
			String json = objectMapper.writeValueAsString(args);
			var e = OutboxEvent.builder()
					.eventType("PAYMENT_REQUESTED_V1")
					.eventKey(txId)
					.payload(json)
					.build();
			return outboxEventRepository.save(e).getId();
		} catch (Exception e) {
			throw new IllegalStateException("outbox serialize failed", e);
		}
	}

	@Transactional
	public void markSent(OutboxEvent e) {
		OutboxEvent updated = e.toBuilder()
				.status("SENT")
				.build();

		outboxEventRepository.save(updated);
	}

	@Transactional
	public void markFailedAndBackoff(OutboxEvent e) {
		int n = e.getAttempts() + 1;
		long sec = Math.min(300, (long) Math.pow(2, Math.min(10, n)));

		OutboxEvent updated = e.toBuilder()
				.attempts(n)
				.nextAttemptAt(LocalDateTime.now().plusSeconds(sec))
				.status("PENDING")
				.build();

		outboxEventRepository.save(updated);
	}
}
