package com.system.payment.payment.integration;

import com.system.payment.common.dto.response.ErrorCode;
import com.system.payment.common.exception.TransientPgException;
import com.system.payment.payment.service.PaymentIdempotencyGuard;
import com.system.payment.payment.service.PaymentProcessService;
import com.system.payment.common.util.KafkaIntegrationTestSupport;
import com.system.payment.common.util.IdGeneratorUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DLT/재시도 정책 통합 테스트
 * - payment.requested.v1 에서 실패 시 payment.requested.v1.DLT 로 라우팅
 */
@SpringBootTest
@EmbeddedKafka(partitions = 2, topics = {"payment.requested.v1", "payment.requested.v1.DLT"})
@TestPropertySource(properties = {
        "spring.kafka.listener.concurrency=1"
})
@ActiveProfiles("test")
// H2/PostgreSQL DB로 충분히 테스트 가능
class PaymentConsumerRetryDltTest extends KafkaIntegrationTestSupport {

    @Override protected String topic() { return "payment.requested.v1"; }

    @MockitoBean
    PaymentProcessService paymentProcessService;

    @MockitoBean
    PaymentIdempotencyGuard idempotencyGuard;

    @Test
    @DisplayName("컨슈머 비재시도: idemKey 공백 → process 미호출, 즉시 DLT")
    void 비재시도_idem_공백() {
        // given
        when(idempotencyGuard.tryAcquire(any())).thenReturn(true); // 호출되면 안 되지만 안전망

        var idem = " "; // 공백 → validateMessagePayload에서 PaymentValidationException 터짐
        var txId = "tx-" + IdGeneratorUtils.UUIDGenerate();

        // when
        send(message(idem, txId, approval(SUCCESS_MOID)));

        // then
        // 컨슈머 레벨에서 예외 발생 → guard, process 모두 NO CALL
        verify(idempotencyGuard, after(1000).never()).tryAcquire(any());
        verify(paymentProcessService, after(1000).never()).process(any());

        // not-retryable → 즉시 DLT에 1건 이상 적재
        var rec = pollOneFromDLT(Duration.ofSeconds(5));
        assertThat(rec).as("DLT 적재").isNotNull();
    }

    @Test
    @DisplayName("서비스 재시도: TransientPgException → 총 6회 호출 후 DLT")
    void 재시도_TransientPgException() {
        // given
        when(idempotencyGuard.tryAcquire(any())).thenReturn(true);

        doThrow(new TransientPgException(ErrorCode.PG_TIMEOUT))
                .when(paymentProcessService).process(any());

        var idem = "idem-" + IdGeneratorUtils.UUIDGenerate();
        var txId = "tx-" + IdGeneratorUtils.UUIDGenerate();

        // when
        send(message(idem, txId, approval(SUCCESS_MOID)));

        // then
        // 원본 1 + 재시도 5 = 총 6회 (백오프 있으니 timeout 여유있게)
        verify(paymentProcessService, timeout(20_000).times(6)).process(any());

        // 재시도 소진 후 DLT 적재
        var rec = pollOneFromDLT(Duration.ofSeconds(10));
        assertThat(rec).as("DLT 적재").isNotNull();
    }
}
