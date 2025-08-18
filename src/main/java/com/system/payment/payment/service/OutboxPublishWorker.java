package com.system.payment.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.payment.domain.outbox.PaymentRequestedArgs;
import com.system.payment.payment.repository.OutboxEventRepository;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxPublishWorker {
	private final OutboxEventRepository outboxEventRepository;
	private final OutboxService outboxService;
	private final PaymentRepository paymentRepository;
	private final PaymentUserCardRepository paymentUserCardRepository;
	private final UserService userService;
	private final PaymentProducer paymentProducer;
	private final ObjectMapper mapper;

	private static final int MAX_ATTEMPTS = 10;

	/**
	 * 개별 이벤트를 신규 트랜잭션(REQUIRES_NEW) 경계에서 처리.
	 * - 성공: markSent
	 * - 실패: markFailedAndBackoff (다음 시도 시각 + 시도횟수 증가)
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processOne(Long eventId) {
		var e = outboxEventRepository.findByIdForUpdate(eventId).orElseThrow();

		// 가드: 대상 상태/타입만 처리
		if (!"PENDING".equals(e.getStatus())) return;
		if (!"PAYMENT_REQUESTED_V1".equals(e.getEventType())) return;

		try {
			var args = mapper.readValue(e.getPayload(), PaymentRequestedArgs.class);
			var payment = paymentRepository.findById(args.paymentId()).orElseThrow();
			var user = userService.findUser();
			var card = paymentUserCardRepository.findById(args.methodId()).orElseThrow();

			paymentProducer.sendPaymentRequested(payment, user, card, args.productName());

			outboxService.markSent(e);
		} catch (Exception ex) {
			// 4) 실패 처리: 이번 실패 반영 시 시도수 계산
			int nextAttempts = e.getAttempts() + 1;

			// 메시지(오류) 길이 제한이 필요하면 잘라서 저장
			String err = truncate(ex.getMessage(), 2000);

			if (nextAttempts >= MAX_ATTEMPTS) {
				// ★ 재시도 한도를 넘겼으므로 DEAD 전환
				e.setStatus("DEAD");
				e.setAttempts(nextAttempts);          // 현재 실패를 반영
				e.setLastError(err);
				// nextAttemptAt 은 의미 없어지나, 남겨도 무방
				outboxEventRepository.save(e);
				// 필요시 로그 남기기
				// log.error("Outbox DEAD. id={}, attempts={}", e.getId(), nextAttempts, ex);
			} else {
				// ★ 아직 한도 미만 → 백오프 적용하여 재시도 스케줄링
				e.setLastError(err);
				outboxService.markFailedAndBackoff(e);
				// log.warn("Outbox publish failed. id={}, attempts={}", e.getId(), nextAttempts, ex);
			}
		}
	}

	private String truncate(String s, int max) {
		if (s == null) return null;
		return s.length() > max ? s.substring(0, max) : s;
	}
}