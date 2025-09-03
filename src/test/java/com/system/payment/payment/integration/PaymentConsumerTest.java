package com.system.payment.payment.integration;

import com.system.payment.payment.service.PaymentIdempotencyGuard;
import com.system.payment.payment.service.PaymentProcessService;
import com.system.payment.util.KafkaIntegrationTestSupport;
import com.system.payment.util.IdGeneratorUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@EmbeddedKafka(partitions = 2, topics = {"payment.requested.v1"})
@ActiveProfiles("test")
class PaymentConsumerTest extends KafkaIntegrationTestSupport {

    @Override protected String topic() { return "payment.requested.v1"; }

    @MockitoBean
    PaymentProcessService paymentProcessService;

    @MockitoBean
    PaymentIdempotencyGuard idempotencyGuard;

    @Test
    @DisplayName("정상 승인 메시지 → Consumer가 process(...)를 1회 호출한다")
    void 정상승인_end2end() {
        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();

        when(idempotencyGuard.tryAcquire(any())).thenReturn(true);

        send(message(idem, txId, approval(SUCCESS_MOID)));

        verify(paymentProcessService, timeout(5_000)).process(any());
        verifyNoMoreInteractions(paymentProcessService);
    }

    @Test
    @DisplayName("비즈니스 실패 메시지(moid에 FAIL 포함) → 예외 없이 process(...)가 호출된다")
    void 비즈니스실패_end2end() {
        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();

        when(idempotencyGuard.tryAcquire(any())).thenReturn(true);

        send(message(idem, txId, approval(FAIL_MOID)));

        verify(paymentProcessService, timeout(5_000)).process(any());
        verifyNoMoreInteractions(paymentProcessService);
    }

    @Test
    @DisplayName("idempotency 획득 실패(tryAcquire=false) → process(...)가 호출되지 않는다")
    void 멱등성키_미획득() {
        // given
        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();

        when(idempotencyGuard.tryAcquire(any())).thenReturn(false);

        // when
        send(message(idem, txId, approval(SUCCESS_MOID)));

        // then
        verify(paymentProcessService, after(1000).never()).process(any());
    }

    @Test
    @DisplayName("밸리데이션 실패(idempotencyKey 공백) → idempotencyGuard/process 미호출")
    void 밸리데이션실패_idempotencyKey() {
        // given
        var idem = " "; // 공백 (validate: isBlank → true)
        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();

        // when
        send(message(idem, txId, approval(SUCCESS_MOID)));

        // then: validate에서 터지므로 guard/process는 절대 호출되면 안 됨
        verify(idempotencyGuard, after(1000).never()).tryAcquire(any());
        verify(paymentProcessService, after(1000).never()).process(any());
    }

    @Test
    @DisplayName("동일 idem 중복 메시지 수신 → 첫 번째만 process 호출, 두 번째는 스킵")
    void 중복메시지_idem() {
        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
        var tx1 = "tx-" + IdGeneratorUtil.UUIDGenerate();
        var tx2 = "tx-" + IdGeneratorUtil.UUIDGenerate();

        // tryAcquire가 순서대로 true → false 리턴하도록 설정
        when(idempotencyGuard.tryAcquire(any()))
                .thenReturn(true)   // 첫 번째 메시지: 획득 성공
                .thenReturn(false); // 두 번째 메시지: 중복으로 획득 실패

        send(message(idem, tx1, approval(SUCCESS_MOID)));
        send(message(idem, tx2, approval(SUCCESS_MOID)));

        // then
        verify(idempotencyGuard, timeout(5_000).times(2)).tryAcquire(any());
        verify(paymentProcessService, timeout(5_000).times(1)).process(any());
        verifyNoMoreInteractions(paymentProcessService);
    }
}
