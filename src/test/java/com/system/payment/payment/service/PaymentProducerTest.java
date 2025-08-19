package com.system.payment.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.payment.domain.Payment;
import com.system.payment.user.domain.PaymentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.net.InetAddress;
import java.time.LocalDateTime;

import static com.system.payment.payment.service.PaymentProducer.PAYMENT_REQUESTED_TOPIC;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProducerTest {

	@Mock
	KafkaTemplate<String, Object> kafkaTemplate;

	@InjectMocks
	PaymentProducer producer;

	private static Payment mockPaymentDeep() {
		// 깊은 스텁으로 중간 객체 타입을 알 필요 없이 체이닝 메서드 스텁 가능
		return mock(Payment.class, Answers.RETURNS_DEEP_STUBS);
	}

	private static PaymentUser mockUser() {
		return mock(PaymentUser.class);
	}

	private static PaymentUserCard mockCard() {
		return mock(PaymentUserCard.class);
	}

	@Test
	@DisplayName("성공: PAYMENT_REQUESTED_TOPIC으로 userId를 key로 하여 메시지를 전송한다")
	void sendPaymentRequested_success_sendsToKafka_withCorrectTopicKeyAndPayload() throws Exception {
		// given
		Payment payment = mockPaymentDeep();
		PaymentUser user = mockUser();
		PaymentUserCard card = mockCard();

		// user/key
		when(user.getId()).thenReturn(1);

		// card
		when(card.getId()).thenReturn(1);
		when(card.getPgOid()).thenReturn("DemoTest_1755064988200");
		when(card.getBillingKey()).thenReturn("BK-XYZ");

		// payment (deep stubs로 체이닝)
		when(payment.getId()).thenReturn(1001);
		when(payment.getTransactionId()).thenReturn("tx-abc-123");
		when(payment.getIdempotencyKey()).thenReturn("idem-001");
		when(payment.getUserRef().getUserId()).thenReturn(1);
		when(payment.getTotalAmount()).thenReturn(1);
		when(payment.getCreatedTimestamp()).thenReturn(LocalDateTime.now());
		when(payment.getPaymentResultCode().getCode()).thenReturn("00");
		// enum.name() 호출 경로까지 한 줄로 스텁
		when(payment.getMethodRef().getPaymentMethodType().name()).thenReturn("CARD");

		// InetAddress.getLocalHost() → 127.0.0.1 (환경 의존성 제거)
		try (MockedStatic<InetAddress> inet = mockStatic(InetAddress.class)) {
			InetAddress fake = mock(InetAddress.class);
			when(fake.getHostAddress()).thenReturn("127.0.0.1");
			inet.when(InetAddress::getLocalHost).thenReturn(fake);

			ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Object> msgCaptor = ArgumentCaptor.forClass(Object.class);

			// when
			producer.sendPaymentRequested(payment, user, card, "AI 라이선스(연 1석)");

			// then
			verify(kafkaTemplate).send(eq(PAYMENT_REQUESTED_TOPIC), keyCaptor.capture(), msgCaptor.capture());

			// 1) key = String.valueOf(userId)
			assertThat(keyCaptor.getValue()).isEqualTo("1");

			// 2) 메시지 내용 스냅샷 검증(JSON으로 최소 핵심 필드 확인)
			ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
			String json = om.writeValueAsString(msgCaptor.getValue());

			// 대표 필드들이 포함되어 있는지(정확한 필드명/구조는 DTO에 따름)
			assertThat(json)
					.contains("payment-api")        // producer
					.contains("tx-abc-123")         // identifiers.transactionId
					.contains("INICIS")             // payload.external.gateway
					.contains("AI 라이선스(연 1석)") // external.raw.data.goodName
					.contains("KRW")                // snapshot.currency
					.contains("1");             // snapshot.amount
		}
	}
}
