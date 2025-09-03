package com.system.payment.payment.integration;

import com.system.payment.exception.PaymentStateTransitionException;
import com.system.payment.payment.domain.*;
import com.system.payment.payment.repository.*;
import com.system.payment.util.IdGeneratorUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentJpaTest {

    // 최소 부트 설정: 베이스 패키지/자동설정/JPA 스캔/감사 켜기
    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.system.payment")
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.system.payment")
    @org.springframework.data.jpa.repository.config.EnableJpaAuditing
    static class TestBootConfig {}

    private static final Integer USER_ID = 1;
    private static final String ORDER_ID = "OID-1";
    private static final Integer ITEM_ID = 1;
    private static final Integer CARD_ID = 101;
    private static final String TID_1 = "TID-1";
    private static final String TID_2 = "TID-2";
    private static final String FAILED_CODE = "PG-400";
    private static final String FAILED_MESSAGE = "카드한도초과";

    // --- 실제 레포지토리 주입 ---
    @Resource PaymentRepository paymentRepository;
    @Resource PaymentDetailRepository paymentDetailRepository;
    @Resource PaymentHistoryRepository paymentHistoryRepository;
    @Resource PaymentIdempotencyRepository paymentIdempotencyRepository;

    // --- 헬퍼: 결제(상세 포함) 생성 ---
    private Payment newPaymentWithDetails(int total, int... itemAmounts) {
        var userRef = PaymentUserRef.of(USER_ID);
        var ref = ReferenceRef.of(ReferenceType.ORDER, ORDER_ID);
        var method = PaymentMethodRef.of(PaymentMethodType.CARD, CARD_ID);

        var p = Payment.builder()
                .userRef(userRef)
                .referenceRef(ref)
                .methodRef(method)
                .paymentType(PaymentType.NORMAL)
                .totalAmount(total)
                .paymentResultCode(PaymentResultCode.WAITING)
                .idempotencyKey("idem-" + IdGeneratorUtil.UUIDGenerate())
                .transactionId("tx-" + IdGeneratorUtil.UUIDGenerate())
                .build();

        for (int amt : itemAmounts) {
            var d = PaymentDetail.create(ItemRef.of(ITEM_ID, ItemType.PRODUCT), amt);  // detail: WAITING 기본값
            p.getDetails().add(d);
        }
        return p;
    }

    @Autowired
    EntityManager em;

    /*
        잘못된 전이 금지 전체 맵: WAITING→COMPLETED 직접 전이 금지, COMPLETED/FAILED→REQUESTED 금지 등
        “허용 전이 표” 전부 파라미터라이즈드 테스트로 커버.
     */
    @Test
    @DisplayName("정상 흐름: WAITING → REQUESTED → COMPLETED (합계·감사필드 검증)")
    void 결제_정상흐름_WAITING에서_COMPLETED까지_합계_감사필드_검증() {
        // given
        var payment = newPaymentWithDetails(3000, 1000, 1000, 1000);
        var saved = paymentRepository.saveAndFlush(payment);

        // then: 초기 상태/감사필드
        assertThat(saved.getPaymentResultCode()).isEqualTo(PaymentResultCode.WAITING);
        assertThat(saved.getCreatedTimestamp()).isNotNull();
        assertThat(saved.getUpdatedTimestamp()).isNotNull();

        // when: WAITING → REQUESTED
        saved.markRequested();
        paymentRepository.saveAndFlush(saved);

        // then: REQUESTED 검증 (DB 왕복 강제)
        em.clear();
        var afterRequested = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterRequested.getPaymentResultCode()).isEqualTo(PaymentResultCode.REQUESTED);
        assertThat(afterRequested.getRequestedTimestamp()).isNotNull();

        // when: REQUESTED → COMPLETED
        var now = LocalDateTime.now();
        afterRequested.markCompleted(TID_1, now);
        paymentRepository.saveAndFlush(afterRequested);

        // then: COMPLETED 검증 (DB 왕복 강제)
        em.clear();
        var reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPaymentResultCode()).isEqualTo(PaymentResultCode.COMPLETED);
        assertThat(reloaded.getApprovedTimestamp()).isNotNull();
        assertThat(reloaded.getExternalPaymentId()).isEqualTo(TID_1);
        assertThat(reloaded.getDetails()).hasSize(3);
    }

    @Test
    @DisplayName("실패 흐름: WAITING → REQUESTED → FAILED (실패 메타 저장 & 승인 메타 없음)")
    void 결제_실패흐름_WAITING에서_FAILED까지_실패메타_검증() {
        // given
        var payment = newPaymentWithDetails(3000, 1000, 1000, 1000);
        var saved = paymentRepository.saveAndFlush(payment);

        // when: WAITING → REQUESTED
        saved.markRequested();
        paymentRepository.saveAndFlush(saved);

        // and: REQUESTED → FAILED
        var now = LocalDateTime.now();
        saved.markFailed(FAILED_CODE, FAILED_MESSAGE, now);
        paymentRepository.saveAndFlush(saved);

        // then
        em.clear();
        var reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPaymentResultCode()).isEqualTo(PaymentResultCode.FAILED);
        assertThat(reloaded.getFailedTimestamp()).isNotNull();
        assertThat(reloaded.getErrorCode()).isEqualTo(FAILED_CODE);
        assertThat(reloaded.getErrorMessage()).isEqualTo(FAILED_MESSAGE);
        assertThat(reloaded.getApprovedTimestamp()).isNull();
        assertThat(reloaded.getExternalPaymentId()).isNull();
        assertThat(reloaded.getDetails()).hasSize(3);
    }

    @Test
    @DisplayName("전이 가드: COMPLETED 이후 REQUESTED 전이는 거부")
    void 완료이후_REQUESTED_전이_거부() {
        // given
        var payment = newPaymentWithDetails(3000, 1000, 1000, 1000);
        var saved = paymentRepository.saveAndFlush(payment);
        saved.markRequested();
        paymentRepository.saveAndFlush(saved);
        saved.markCompleted(TID_1, LocalDateTime.now());
        paymentRepository.saveAndFlush(saved);
        em.clear();

        // when & then
        var reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThatThrownBy(() -> {
            reloaded.markRequested();
            paymentRepository.saveAndFlush(reloaded);
        }).isInstanceOf(PaymentStateTransitionException.class);
    }

    @Test
    @DisplayName("전이 가드: FAILED 이후 COMPLETED 전이는 거부(FAILED는 종단)")
    void 실패이후_COMPLETED_전이_거부() {
        // given
        var payment = newPaymentWithDetails(3000, 1000, 1000, 1000);
        var saved = paymentRepository.saveAndFlush(payment);
        saved.markRequested();
        paymentRepository.saveAndFlush(saved);
        saved.markFailed(FAILED_CODE, FAILED_MESSAGE, LocalDateTime.now());
        paymentRepository.saveAndFlush(saved);
        em.clear();

        // when & then
        var failed = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThatThrownBy(() -> {
            failed.markCompleted(TID_1, LocalDateTime.now());
            paymentRepository.saveAndFlush(failed);
        }).isInstanceOf(PaymentStateTransitionException.class);
    }

    @Test
    @DisplayName("합계 불일치: REQUESTED에서 COMPLETED 시도 → 예외, 상태는 REQUESTED 유지(롤백)")
    void 합계불일치_완료_예외_상태유지() {
        // given: 총액 3000, 상세합 2000(의도적 불일치)
        var payment = newPaymentWithDetails(3000, 1000, 1000);
        var saved = paymentRepository.saveAndFlush(payment);

        // when: WAITING → REQUESTED
        saved.markRequested();
        paymentRepository.saveAndFlush(saved);

        // then: REQUESTED에서 COMPLETED 시 합계 검증 실패 → 예외 & 롤백
        assertThatThrownBy(() -> {
            saved.markCompleted(TID_1, LocalDateTime.now());
            paymentRepository.saveAndFlush(saved);
        }).isInstanceOf(PaymentStateTransitionException.class);

        // and then: DB 왕복 후 상태/메타 확인 (여전히 REQUESTED, 승인 메타 없음)
        em.clear();
        var reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPaymentResultCode()).isEqualTo(PaymentResultCode.REQUESTED);
        assertThat(reloaded.getApprovedTimestamp()).isNull();
        assertThat(reloaded.getExternalPaymentId()).isNull();
    }

    @Test
    @DisplayName("중복 승인 방지: COMPLETED 상태에서 다시 markCompleted 호출 시 예외 및 메타 불변")
    void 중복승인_예외_및_메타불변() {
        // given: 총액=상세합(정상) 결제 생성
        var payment = newPaymentWithDetails(3000, 1000, 1000, 1000);
        var saved = paymentRepository.saveAndFlush(payment);

        // when: WAITING → REQUESTED
        saved.markRequested();
        paymentRepository.saveAndFlush(saved);

        // and: REQUESTED → COMPLETED (정상 승인 1회차)
        var firstApprovedAt = LocalDateTime.now().minusSeconds(1);
        var firstTid = TID_1;
        saved.markCompleted(firstTid, firstApprovedAt);
        paymentRepository.saveAndFlush(saved);

        // DB 왕복 후 승인 메타 확인
        em.clear();
        var completed = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(completed.getPaymentResultCode()).isEqualTo(PaymentResultCode.COMPLETED);
        assertThat(completed.getExternalPaymentId()).isEqualTo(firstTid);
        assertThat(completed.getApprovedTimestamp())
                .isCloseTo(firstApprovedAt, within(1, ChronoUnit.MICROS));

        // then: COMPLETED 상태에서 다시 승인 시도 → 예외
        var secondTid = TID_2;                  // 만약 덮어쓰면 안 됨
        var secondApprovedAt = LocalDateTime.now();    // 만약 시간 갱신되면 안 됨
        assertThatThrownBy(() -> {
            completed.markCompleted(secondTid, secondApprovedAt);
            paymentRepository.saveAndFlush(completed);
        }).isInstanceOf(PaymentStateTransitionException.class);

        // and then: DB 왕복 후에도 메타 불변(첫 승인 그대로 유지)
        em.clear();
        var reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPaymentResultCode()).isEqualTo(PaymentResultCode.COMPLETED);
        assertThat(reloaded.getExternalPaymentId()).isEqualTo(firstTid);
        assertThat(reloaded.getApprovedTimestamp())
                .isCloseTo(firstApprovedAt, within(1, ChronoUnit.MICROS));
    }
}
