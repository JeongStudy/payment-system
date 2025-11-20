package com.system.payment.payment.integration;

import com.system.payment.config.TestBootConfig;
import com.system.payment.exception.PaymentStateTransitionException;
import com.system.payment.payment.domain.*;
import com.system.payment.payment.repository.PaymentDetailRepository;
import com.system.payment.payment.repository.PaymentIdempotencyRepository;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.payment.service.PaymentIdempotencyGuard;
import com.system.payment.util.IdGeneratorUtils;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = TestBootConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration")
// 동시성 테스트를 위해선 H2/PostgreSQL이 아닌 testContainer를 사용해야 함(integration profile에서 설정)
class PaymentJpaConcurrencyTest {

    private static final Integer USER_ID = 1;
    private static final String ORDER_ID = "OID-CC";
    private static final Integer ITEM_ID = 1;
    private static final Integer CARD_ID = 101;

    @Resource PaymentRepository paymentRepository;
    @Resource PaymentDetailRepository paymentDetailRepository;
    @Resource PaymentIdempotencyRepository paymentIdempotencyRepository;

    @Autowired PlatformTransactionManager txManager;

    // --- 헬퍼: 결제(상세 포함) 생성 ---
    private Payment createPaymentWithDetails(int total, int... itemAmounts) {
        var p = Payment.builder()
                .userRef(PaymentUserRef.of(USER_ID))
                .referenceRef(ReferenceRef.of(ReferenceType.ORDER, ORDER_ID))
                .methodRef(PaymentMethodRef.of(PaymentMethodType.CARD, CARD_ID))
                .paymentType(PaymentType.NORMAL)
                .totalAmount(total)
                .paymentResultCode(PaymentResultCode.WAITING)
                .idempotencyKey("idem-" + IdGeneratorUtils.UUIDGenerate())
                .transactionId("tx-" + IdGeneratorUtils.UUIDGenerate())
                .build();
        for (int amt : itemAmounts) {
            p.getDetails().add(PaymentDetail.create(ItemRef.of(ITEM_ID, ItemType.PRODUCT), amt));
        }
        return p;
    }

    // 동시 승인 경합: 테스트에서만 PESSIMISTIC_WRITE 잠금으로 직렬화 보장
    @Test
    @DisplayName("동시 승인: 같은 Payment를 동시에 COMPLETED 시도 → 한쪽만 성공, 다른 한쪽은 전이 가드 예외")
    void 동시승인_한쪽만성공_다른한쪽예외() throws Exception {
        // given: 총액=상세합, REQUESTED 상태로 준비
        var payment = paymentRepository.saveAndFlush(createPaymentWithDetails(3000, 1000, 1000, 1000));

        new TransactionTemplate(txManager).execute(s -> {
            var p = paymentRepository.findById(payment.getId()).orElseThrow();
            p.markRequested();
            paymentRepository.saveAndFlush(p);
            return null;
        });

        // when: 두 스레드가 거의 동시에 COMPLETED 시도
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Callable<String> t1 = () -> new TransactionTemplate(txManager).execute(s -> {
            ready.countDown();
            try {
                if (!go.await(2, TimeUnit.SECONDS)) { // 스타트 게이트 타임아웃 안전장치
                    throw new IllegalStateException("start gate timed out");
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }

            var p = paymentRepository.findByIdForUpdate(payment.getId()) // 🔒 비관적 락으로 직렬화
                    .orElseThrow();
            p.markCompleted("TID-1", LocalDateTime.now());
            paymentRepository.saveAndFlush(p);
            return "T1-OK";
        });

        Callable<String> t2 = () -> new TransactionTemplate(txManager).execute(s -> {
            ready.countDown();
            try {
                if (!go.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("start gate timed out");
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }

            var p = paymentRepository.findByIdForUpdate(payment.getId()) // 🔒 비관적 락으로 직렬화
                    .orElseThrow();
            p.markCompleted("TID-2", LocalDateTime.now());
            paymentRepository.saveAndFlush(p);
            return "T2-OK";
        });

        Future<String> f1 = pool.submit(t1);
        Future<String> f2 = pool.submit(t2);

        // 두 작업이 준비될 때까지 대기 → 동시에 출발
        assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
        go.countDown();

        String r1 = null, r2 = null;
        Throwable e1 = null, e2 = null;

        try { r1 = f1.get(5, TimeUnit.SECONDS); } catch (Throwable t) { e1 = t; }
        try { r2 = f2.get(5, TimeUnit.SECONDS); } catch (Throwable t) { e2 = t; }

        // 종료 절차 (shutdownNow 대신 정상 종료 시도 → 인터럽트로 인한 실패 방지)
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);

        // then: 정확히 한 건만 성공해야 함 (다른 한쪽은 PaymentStateTransitionException)
        if ("T1-OK".equals(r1) && e2 != null) {
            assertThat(rootCause(e2)).isInstanceOf(PaymentStateTransitionException.class);
        } else if ("T2-OK".equals(r2) && e1 != null) {
            assertThat(rootCause(e1)).isInstanceOf(PaymentStateTransitionException.class);
        } else {
            fail("동시 승인 결과가 비정상 (둘 다 성공/실패). r1=" + r1 + ", r2=" + r2 + ", e1=" + e1 + ", e2=" + e2);
        }

        // 최종 상태 1회 승인 확인
        var finalP = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(finalP.getPaymentResultCode()).isEqualTo(PaymentResultCode.COMPLETED);
        assertThat(finalP.getExternalPaymentId()).isIn("TID-1", "TID-2");
    }
    
    // 멱등성 가드 경합: 같은 키 동시 tryAcquire → 정확히 1회만 true
    @Test
    @DisplayName("IdempotencyGuard: 같은 키 동시 획득 시 정확히 1회만 true")
    void 멱등가드_동시획득_정확히한번성공() throws Exception {
        var guard = new PaymentIdempotencyGuard(paymentIdempotencyRepository);
        String key = "idem-" + IdGeneratorUtils.UUIDGenerate();

        ExecutorService pool = Executors.newFixedThreadPool(4);
        Callable<Boolean> c = () -> guard.tryAcquire(key);

        var f1 = pool.submit(c);
        var f2 = pool.submit(c);
        var f3 = pool.submit(c);
        var f4 = pool.submit(c);

        int success = 0;
        if (f1.get()) success++;
        if (f2.get()) success++;
        if (f3.get()) success++;
        if (f4.get()) success++;

        pool.shutdownNow();
        assertThat(success).isEqualTo(1);
    }

    private static Throwable rootCause(Throwable t) {
        Throwable c = t;
        while (c != null && c.getCause() != null) c = c.getCause();
        return c;
    }
}
