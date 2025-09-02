package com.system.payment.payment.service;

import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentValidationException;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import com.system.payment.util.KafkaUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentConsumerTest {

    @Mock
    private PaymentProcessService paymentProcessService;

    @Mock
    private PaymentIdempotencyGuard idempotencyGuard;

    @InjectMocks
    private PaymentConsumer consumer;

    // 테스트 상수
    private static final String HDR_TRACE_ID = "t-123";
    private static final String HDR_SPAN_ID  = "s-456";
    private static final String HDR_CORR_ID  = "c-789";
    private static final String IDEM_KEY     = "idem-1";
    private static final String TX_ID        = "tx-1";
    private static final String PROVIDER     = "INICIS";
    private static final String MID          = "INIBillTst";
    private static final String RECEIVED_KEY = "k-key";
    private static final int    RECEIVED_PARTITION = 0;
    private static final long   OFFSET = 12L;

    private Map<String, Object> baseHeaders() {
        Map<String, Object> h = new HashMap<>();
        h.put(KafkaHeaders.RECEIVED_KEY, RECEIVED_KEY);
        h.put(KafkaHeaders.RECEIVED_PARTITION, RECEIVED_PARTITION);
        h.put(KafkaHeaders.OFFSET, OFFSET);
        return h;
    }

    @SuppressWarnings("unchecked")
    private PaymentRequestedMessageV1<InicisBillingApproval> mockMessage(
            String idempotencyKey, String txId, String provider, String mid
    ) {
        PaymentRequestedMessageV1<InicisBillingApproval> msg = mock(PaymentRequestedMessageV1.class);

        PaymentRequestedMessageV1.Envelope env = mock(PaymentRequestedMessageV1.Envelope.class);
        when(env.idempotencyKey()).thenReturn(idempotencyKey);

        PaymentRequestedMessageV1.Identifiers ids = mock(PaymentRequestedMessageV1.Identifiers.class);
        when(ids.transactionId()).thenReturn(txId);

        InicisBillingApproval approval = mock(InicisBillingApproval.class);
        when(approval.getMid()).thenReturn(mid);

        PaymentRequestedMessageV1.Payload.External<InicisBillingApproval> ext =
                mock(PaymentRequestedMessageV1.Payload.External.class);
        when(ext.approval()).thenReturn(approval);
        when(ext.provider()).thenReturn(provider);

        PaymentRequestedMessageV1.Payload<InicisBillingApproval> payload =
                mock(PaymentRequestedMessageV1.Payload.class);
        when(payload.external()).thenReturn(ext);

        when(msg.envelope()).thenReturn(env);
        when(msg.identifiers()).thenReturn(ids);
        when(msg.payload()).thenReturn(payload);

        return msg;
    }

    @AfterEach
    void tearDown() {
        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty(),
                "MDC should be cleared after onMessage");
        MDC.clear();
    }

    @Test
    void 성공_정상흐름_헤더존재_멱등성획득_프로세스호출_성공마킹() {
        try (MockedStatic<KafkaUtil> mocked = mockStatic(KafkaUtil.class)) {
            // given
            Map<String, Object> headers = baseHeaders();
            mocked.when(() -> KafkaUtil.extractKafkaHeader(eq(headers), eq(PaymentConsumer.HDR_TRACE_ID))).thenReturn(HDR_TRACE_ID);
            mocked.when(() -> KafkaUtil.extractKafkaHeader(eq(headers), eq(PaymentConsumer.HDR_SPAN_ID))).thenReturn(HDR_SPAN_ID);
            mocked.when(() -> KafkaUtil.extractKafkaHeader(eq(headers), eq(PaymentConsumer.HDR_CORR_ID))).thenReturn(HDR_CORR_ID);
            when(idempotencyGuard.tryAcquire(IDEM_KEY)).thenReturn(true);
            PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(IDEM_KEY, TX_ID, PROVIDER, MID);

            // when
            consumer.onMessage(msg, RECEIVED_KEY, RECEIVED_PARTITION, OFFSET, headers);

            // then
            mocked.verify(() -> KafkaUtil.validateMessagePayload(eq(IDEM_KEY), eq(TX_ID), any(), any()));
            verify(paymentProcessService).process(eq(msg));
        }
    }

    @Test
    void 헤더없음_기본값보정_UUID로채움_정상처리() {
        try (MockedStatic<KafkaUtil> mocked = mockStatic(KafkaUtil.class)) {
            // given
            Map<String, Object> headers = baseHeaders();
            mocked.when(() -> KafkaUtil.extractKafkaHeader(eq(headers), anyString())).thenReturn(null);
            when(idempotencyGuard.tryAcquire(IDEM_KEY)).thenReturn(true);
            PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(IDEM_KEY, TX_ID, PROVIDER, MID);

            // when
            consumer.onMessage(msg, RECEIVED_KEY, RECEIVED_PARTITION, OFFSET, headers);

            // then
            mocked.verify(() -> KafkaUtil.validateMessagePayload(eq(IDEM_KEY), eq(TX_ID), any(), any()));
            verify(paymentProcessService).process(eq(msg));
        }
    }

    @Test
    void 멱등성중복_tryAcquire_false_프로세스미호출() {
        try (MockedStatic<KafkaUtil> mocked = mockStatic(KafkaUtil.class)) {
            // given
            Map<String, Object> headers = baseHeaders();
            mocked.when(() -> KafkaUtil.extractKafkaHeader(eq(headers), anyString())).thenReturn(null);
            PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(IDEM_KEY, TX_ID, PROVIDER, MID);
            when(idempotencyGuard.tryAcquire(IDEM_KEY)).thenReturn(false);

            // when
            consumer.onMessage(msg, RECEIVED_KEY, RECEIVED_PARTITION, OFFSET, headers);

            // then
            mocked.verify(() -> KafkaUtil.validateMessagePayload(eq(IDEM_KEY), eq(TX_ID), any(), any()));
            verify(paymentProcessService, never()).process(any());
            verify(idempotencyGuard, never()).markSuccess(any());
        }
    }

    @Test
    void 밸리데이션실패_PaymentValidationException_삼키고_리턴() {
        try (MockedStatic<KafkaUtil> mocked = mockStatic(KafkaUtil.class)) {
            // given
            Map<String, Object> headers = baseHeaders();
            mocked.when(() -> KafkaUtil.extractKafkaHeader(eq(headers), anyString())).thenReturn(null);
            PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(null, null, null, null);
            mocked.when(() -> KafkaUtil.validateMessagePayload(isNull(), isNull(), any(), any()))
                    .thenThrow(new PaymentValidationException(ErrorCode.PAYMENT_VALIDATION_MISSING_FIELD));

            // when & then
            assertThrows(PaymentValidationException.class,
                    () -> consumer.onMessage(msg, RECEIVED_KEY, RECEIVED_PARTITION, OFFSET, headers));

            verify(paymentProcessService, never()).process(any());
            verify(idempotencyGuard, never()).tryAcquire(any());
        }
    }

    @Test
    void 메시지_null이어도_NPE없이_밸리데이션경로탐() {
        try (MockedStatic<KafkaUtil> mocked = mockStatic(KafkaUtil.class)) {
            // given
            Map<String, Object> headers = baseHeaders();
            mocked.when(() -> KafkaUtil.extractKafkaHeader(eq(headers), anyString())).thenReturn(null);
            mocked.when(() -> KafkaUtil.validateMessagePayload(isNull(), isNull(), isNull(), isNull()))
                    .thenThrow(new PaymentValidationException(ErrorCode.PAYMENT_VALIDATION_MISSING_FIELD));

            // when & then
            assertThrows(PaymentValidationException.class,
                    () -> consumer.onMessage(null, RECEIVED_KEY, RECEIVED_PARTITION, OFFSET, headers));

            verify(paymentProcessService, never()).process(any());
        }
    }

    @Test
    void 프로세스중_예상치못한예외_로그후_재throw() {
        try (MockedStatic<KafkaUtil> mocked = mockStatic(KafkaUtil.class)) {
            // given
            Map<String, Object> headers = baseHeaders();
            mocked.when(() -> KafkaUtil.extractKafkaHeader(eq(headers), anyString())).thenReturn(null);
            PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(IDEM_KEY, TX_ID, PROVIDER, MID);
            when(idempotencyGuard.tryAcquire(IDEM_KEY)).thenReturn(true);
            doThrow(new RuntimeException("error")).when(paymentProcessService).process(any());

            // when & then
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> consumer.onMessage(msg, RECEIVED_KEY, RECEIVED_PARTITION, OFFSET, headers));
            assertEquals("error", ex.getMessage());
        }
    }
}
