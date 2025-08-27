package com.system.payment.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.payment.domain.outbox.EventType;
import com.system.payment.payment.domain.outbox.OutboxEvent;
import com.system.payment.payment.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

	@Mock
	OutboxEventRepository outboxEventRepository;

	@InjectMocks
	OutboxService outboxService;

	ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("enqueuePaymentRequested: OutboxEvent(eventType, eventKey, payload)가 올바르게 저장된다")
	void enqueuePaymentRequested_savesCorrectOutboxEvent() throws Exception {
		// given
		Integer paymentId = 1;
		String txId = "tx-abc-001";
		Integer userId = 1;
		String methodType = "CARD";
		Integer methodId = 1;
		String productName = "AI 라이센스 키(연 1석)";

		when(outboxEventRepository.save(any(OutboxEvent.class)))
				.thenAnswer(invocation -> {
					OutboxEvent e = invocation.getArgument(0);
					e.setId(1); // 테스트 검증용 ID
					return e;
				});

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

		// when
		outboxService.enqueuePaymentRequested(paymentId, txId, userId, methodType, methodId, productName);


		// then
		verify(outboxEventRepository).save(captor.capture());
		OutboxEvent saved = captor.getValue();

		assertThat(saved).isNotNull();
		assertThat(saved.getEventType()).isEqualTo(EventType.PAYMENT_REQUESTED_V1); // from service
		assertThat(saved.getEventKey()).isEqualTo(txId);                    // txId -> eventKey
		assertThat(saved.getPayload()).isNotBlank();

		// payload 검증(JSON → Map)
		Map<String, Object> payload = objectMapper.readValue(
				saved.getPayload(), new TypeReference<Map<String, Object>>() {
				}
		);

		// 숫자(Object → Number → int) 캐스팅 유의
		assertThat(((Number) payload.get("paymentId")).intValue()).isEqualTo(paymentId);
		// record 필드명은 transactionId 임(서비스가 이 구조로 직렬화함)
		assertThat(payload.get("transactionId")).isEqualTo(txId);
		assertThat(((Number) payload.get("userId")).intValue()).isEqualTo(userId);
		assertThat(payload.get("methodType")).isEqualTo(methodType);
		assertThat(((Number) payload.get("methodId")).intValue()).isEqualTo(methodId);
		assertThat(payload.get("productName")).isEqualTo(productName);
	}

	@Test
	@DisplayName("markSent: 상태를 SENT로 설정하고 저장한다")
	void markSent_updatesStatusToSentAndSaves() {
		// given
		OutboxEvent e = new OutboxEvent();
		e.setStatus("PENDING");

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

		// when
		outboxService.markSent(e);

		// then
		verify(outboxEventRepository).save(captor.capture());
		OutboxEvent saved = captor.getValue();
		assertThat(saved.getStatus()).isEqualTo("SENT");
	}

	@Test
	@DisplayName("markFailedAndBackoff: attempts를 +1 하고 지수 백오프(초기 2초)를 적용한다")
	void markFailedAndBackoff_incrementsAttempts_andAppliesExponentialBackoff_initial() {
		// given: 현재 attempts = 0 → 증가 후 1 → 지수백오프 2초
		OutboxEvent e = new OutboxEvent();
		e.setAttempts(0);       // @PrePersist가 실행되지 않으므로 테스트에서 직접 세팅
		e.setStatus("PENDING"); // 동일

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		LocalDateTime before = LocalDateTime.now();

		// when
		outboxService.markFailedAndBackoff(e);

		// then
		LocalDateTime after = LocalDateTime.now();
		verify(outboxEventRepository).save(captor.capture());
		OutboxEvent saved = captor.getValue();

		assertThat(saved.getAttempts()).isEqualTo(1);
		assertThat(saved.getStatus()).isEqualTo("PENDING");

		long expectedSec = 2L; // 2^1
		assertThat(saved.getNextAttemptAt())
				.isAfterOrEqualTo(before.plusSeconds(expectedSec - 1))
				.isBeforeOrEqualTo(after.plusSeconds(expectedSec + 1));
	}

	@Test
	@DisplayName("markFailedAndBackoff: 백오프는 최대 300초로 캡된다(시도 9→10에서 300초)")
	void markFailedAndBackoff_capsBackoffAt300Seconds() {
		// given: attempts = 9 → 증가 후 10 → 2^10=1024 하지만 cap 300초
		OutboxEvent e = new OutboxEvent();
		e.setAttempts(9);
		e.setStatus("PENDING");

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		LocalDateTime before = LocalDateTime.now();

		// when
		outboxService.markFailedAndBackoff(e);

		// then
		LocalDateTime after = LocalDateTime.now();
		verify(outboxEventRepository).save(captor.capture());
		OutboxEvent saved = captor.getValue();

		assertThat(saved.getAttempts()).isEqualTo(10);
		assertThat(saved.getStatus()).isEqualTo("PENDING");

		long expectedSec = 300L; // cap
		assertThat(saved.getNextAttemptAt())
				.isAfterOrEqualTo(before.plusSeconds(expectedSec - 1))
				.isBeforeOrEqualTo(after.plusSeconds(expectedSec + 1));
	}
}
