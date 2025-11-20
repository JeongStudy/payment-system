package com.system.payment.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.domain.entity.PaymentUserCard;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.outbox.service.OutboxService;
import com.system.payment.payment.domain.entity.Payment;
import com.system.payment.outbox.domain.constant.EventType;
import com.system.payment.outbox.domain.entity.OutboxEvent;
import com.system.payment.outbox.model.dto.PaymentRequestedArgs;
import com.system.payment.outbox.repository.OutboxEventRepository;
import com.system.payment.payment.producer.PaymentProducer;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.outbox.worker.OutboxPublishWorker;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OutboxPublishWorker.processOne 서비스 테스트
 * <p>
 * 검증 항목:
 * 1) 성공 시: paymentProducer 호출 후 outboxService.markSent 호출
 * 2) 실패(시도<MAX): outboxService.markFailedAndBackoff 호출 + lastError 세팅
 * 3) 실패(시도>=MAX): 상태 DEAD/attempts 증가 후 outboxEventRepository.save 호출
 * 4) PENDING 아님/타입 불일치: 아무 동작도 하지 않음
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublishWorkerTest {

	@Mock
	OutboxEventRepository outboxEventRepository;
	@Mock
	PaymentUserRepository paymentUserRepository;
	@Mock
	OutboxService outboxService;
	@Mock
	PaymentRepository paymentRepository;
	@Mock
	PaymentUserCardRepository paymentUserCardRepository;
	@Mock
	UserService userService;
	@Mock
	PaymentProducer paymentProducer;
	@Mock
	ObjectMapper objectMapper;

	@InjectMocks
	OutboxPublishWorker worker;

	private static OutboxEvent newEvent(Integer id, String status, EventType type, String payload, int attempts) {
		OutboxEvent e = new OutboxEvent();
		e.setId(id);
		e.setStatus(status);
		e.setEventType(type);
		e.setPayload(payload);
		e.setAttempts(attempts);
		return e;
	}

	private static PaymentRequestedArgs args(int paymentId, String txId, int userId, String methodType, int methodId, String productName) {
		return new PaymentRequestedArgs(paymentId, txId, userId, methodType, methodId, productName);
	}

	// 공통 더미 도메인 (프로듀서는 mock이라 내부 게터 호출 안 됨)
	private static Payment dummyPayment() {
		return mock(Payment.class);
	}

	private static PaymentUser dummyUser() {
		return mock(PaymentUser.class);
	}

	private static PaymentUserCard dummyCard() {
		return mock(PaymentUserCard.class);
	}

	@Test
	@DisplayName("성공 플로우: Producer 전송 후 markSent 호출")
	void processOne_success_marksSent() throws Exception {
		// given
		Integer id = 10;
		OutboxEvent e = newEvent(id, "PENDING", EventType.PAYMENT_REQUESTED_V1, "{}", 0);

		when(outboxEventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(e));
		when(objectMapper.readValue(anyString(), eq(PaymentRequestedArgs.class)))
				.thenReturn(args(1, "tx-abc-001", 1, "CARD", 1, "AI 라이센스 키(연 1석)"));
		when(paymentRepository.findById(1)).thenReturn(Optional.of(dummyPayment()));
		when(paymentUserRepository.getByIdOrThrow(1)).thenReturn(dummyUser());
		when(paymentUserCardRepository.findById(1)).thenReturn(Optional.of(dummyCard()));

		// when
		worker.processOne(id);

		// then
		verify(paymentProducer).sendPaymentRequested(any(), any(), any(), eq("AI 라이센스 키(연 1석)"));
		verify(outboxService).markSent(same(e));
		verify(outboxEventRepository, never()).save(any()); // DEAD 경로 아님
	}

	@Test
	@DisplayName("실패(시도<MAX): markFailedAndBackoff 호출 및 lastError 세팅")
	void processOne_failure_underMax_callsBackoff() throws Exception {
		// given
		Integer id = 11;
		OutboxEvent e = newEvent(id, "PENDING", EventType.PAYMENT_REQUESTED_V1, "{}", 0);

		when(outboxEventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(e));
		when(objectMapper.readValue(anyString(), eq(PaymentRequestedArgs.class)))
				.thenReturn(args(1, "tx-abc-001", 1, "CARD", 1, "AI 라이센스 키(연 1석)"));
		when(paymentRepository.findById(1)).thenReturn(Optional.of(dummyPayment()));
		when(paymentUserRepository.getByIdOrThrow(1)).thenReturn(dummyUser());
		when(paymentUserCardRepository.findById(1)).thenReturn(Optional.of(dummyCard()));

		// 프로듀서에서 일시 실패 발생
		doThrow(new RuntimeException("temporary failure"))
				.when(paymentProducer).sendPaymentRequested(any(), any(), any(), anyString());

		// when
		worker.processOne(id);

		// then
		// lastError는 worker가 직접 세팅
		assertThat(e.getLastError()).contains("temporary failure");
		// 백오프는 서비스로 위임
		verify(outboxService).markFailedAndBackoff(same(e));
		// DEAD 저장 경로는 아님
		verify(outboxEventRepository, never()).save(any());
		verify(outboxService, never()).markSent(any());
	}

	@Test
	@DisplayName("실패(시도>=MAX): DEAD 전환 및 저장")
	void processOne_failure_reachMax_setsDeadAndSaves() throws Exception {
		// given
		Integer id = 12;
		OutboxEvent e = newEvent(id, "PENDING", EventType.PAYMENT_REQUESTED_V1, "{}", 9); // attempts=9 → next=10

		when(outboxEventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(e));
		when(objectMapper.readValue(anyString(), eq(PaymentRequestedArgs.class)))
				.thenReturn(args(1, "tx-abc-001", 1, "CARD", 1, "AI 라이센스 키(연 1석)"));
		when(paymentRepository.findById(1)).thenReturn(Optional.of(dummyPayment()));
		when(paymentUserRepository.getByIdOrThrow(1)).thenReturn(dummyUser());
		when(paymentUserCardRepository.findById(1)).thenReturn(Optional.of(dummyCard()));

		doThrow(new RuntimeException("permanent failure"))
				.when(paymentProducer).sendPaymentRequested(any(), any(), any(), anyString());

		ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);

		// when
		worker.processOne(id);

		// then
		verify(outboxEventRepository).save(saved.capture());
		OutboxEvent after = saved.getValue();

		assertThat(after.getStatus()).isEqualTo("DEAD");
		assertThat(after.getAttempts()).isEqualTo(10);          // 실패 반영
		assertThat(after.getLastError()).contains("permanent failure");

		// 백오프 호출/성공 처리 없음
		verify(outboxService, never()).markFailedAndBackoff(any());
		verify(outboxService, never()).markSent(any());
	}

	@Test
	@DisplayName("스킵: PENDING이 아니면 처리하지 않음")
	void processOne_skips_whenNotPending() {
		// given
		Integer id = 13;
		OutboxEvent e = newEvent(id, "SENT", EventType.PAYMENT_REQUESTED_V1, "{}", 0);
		when(outboxEventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(e));

		// when
		worker.processOne(id);

		// then
		verifyNoInteractions(paymentProducer, outboxService, paymentRepository, paymentUserCardRepository, userService);
	}

}
