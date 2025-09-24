package com.system.payment.payment.integration;

import com.system.payment.config.TestBootConfig;
import com.system.payment.exception.PaymentStateTransitionException;
import com.system.payment.payment.domain.*;
import com.system.payment.payment.repository.*;
import com.system.payment.util.IdGeneratorUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ActiveProfiles("test")
@DataJpaTest
@ContextConfiguration(classes = TestBootConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// H2/PostgreSQL DB로 충분히 테스트 가능
class PaymentJpaTransitionTest {

    private static final Integer USER_ID = 1;
    private static final String ORDER_ID = "OID-1";
    private static final Integer ITEM_ID = 1;
    private static final Integer CARD_ID = 101;
    private static final String TID_1 = "TID-1";
    private static final String TID_2 = "TID-2";
    private static final String FAILED_CODE = "PG-400";
    private static final String FAILED_MESSAGE = "카드한도초과";

    @Resource PaymentRepository paymentRepository;

    @Autowired EntityManager em;

    // --- 헬퍼: 결제(상세 포함) 생성(합계 일치) ---
    private Payment newPaymentWithDetails(int total, int... itemAmounts) {
        PaymentUserRef userRef = PaymentUserRef.of(USER_ID);
        ReferenceRef ref = ReferenceRef.of(ReferenceType.ORDER, ORDER_ID);
        PaymentMethodRef method = PaymentMethodRef.of(PaymentMethodType.CARD, CARD_ID);

        Payment payment = Payment.builder()
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
            payment.getDetails().add(PaymentDetail.create(ItemRef.of(ITEM_ID, ItemType.PRODUCT), amt));
        }
        return payment;
    }

    // ========= 표 기반 파라미터라이즈드 =========

    @DisplayName("전이 규칙 표 검증")
    @ParameterizedTest(name = "{index}) {0} → {1} : allowed={2}")
    @MethodSource("transitionCases")
    void 전이_규칙_표_검증(PaymentResultCode from, PaymentResultCode to, boolean allowed) {
        // given: 합계 일치(3000=1000+1000+1000)
        Payment payment = newPaymentWithDetails(3000, 1000, 1000, 1000);
        payment = paymentRepository.saveAndFlush(payment);

        // 초기 상태를 from으로 만들어 둔다
        moveToState(payment, from);  // WAITING이면 no-op, 나머지는 단계적 전이로 도달
        paymentRepository.saveAndFlush(payment);
        em.clear();

        // when/then
        Payment reloadPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        if (allowed) {
            performTransition(reloadPayment, to);
            paymentRepository.saveAndFlush(reloadPayment);

            em.clear();
            Payment after = paymentRepository.findById(payment.getId()).orElseThrow();
            assertThat(after.getPaymentResultCode()).isEqualTo(to);
        } else {
            assertThatThrownBy(() -> {
                performTransition(reloadPayment, to);
                paymentRepository.saveAndFlush(reloadPayment);
            }).isInstanceOf(PaymentStateTransitionException.class);
        }
    }

    private static Stream<Arguments> transitionCases() {
        return Stream.of(
                // 허용
                arguments(PaymentResultCode.WAITING,   PaymentResultCode.REQUESTED, true),
                arguments(PaymentResultCode.REQUESTED, PaymentResultCode.COMPLETED, true),
                arguments(PaymentResultCode.REQUESTED, PaymentResultCode.FAILED,    true),

                // 금지
                arguments(PaymentResultCode.WAITING,   PaymentResultCode.COMPLETED, false),
                arguments(PaymentResultCode.COMPLETED, PaymentResultCode.REQUESTED, false),
                arguments(PaymentResultCode.COMPLETED, PaymentResultCode.FAILED,    false),
                arguments(PaymentResultCode.FAILED,    PaymentResultCode.REQUESTED, false),
                arguments(PaymentResultCode.FAILED,    PaymentResultCode.COMPLETED, false)
        );
    }

    /** from 상태로 이동(도메인 규칙을 어기지 않도록 합법 전이만 사용) */
    private void moveToState(Payment p, PaymentResultCode from) {
        switch (from) {
            case WAITING -> { /* no-op */ }
            case REQUESTED -> p.markRequested(); // WAITING -> REQUESTED
            case COMPLETED -> {                  // WAITING -> REQUESTED -> COMPLETED
                p.markRequested();
                p.markCompleted(TID_1, LocalDateTime.now());
            }
            case FAILED -> {                     // WAITING -> REQUESTED -> FAILED
                p.markRequested();
                p.markFailed(FAILED_CODE, FAILED_MESSAGE, LocalDateTime.now());
            }
        }
    }

    /** to 상태 전이 시도 */
    private void performTransition(Payment p, PaymentResultCode to) {
        switch (to) {
            case REQUESTED -> p.markRequested();
            case COMPLETED -> p.markCompleted(TID_2, LocalDateTime.now());
            case FAILED    -> p.markFailed(FAILED_CODE, FAILED_MESSAGE, LocalDateTime.now());
            case WAITING   -> throw new IllegalArgumentException("역전이(WAITING으로)는 지원하지 않음");
        }
    }

    // ========= 기존 개별 테스트(보강용) =========

    @Test
    @DisplayName("정상 흐름: WAITING → REQUESTED → COMPLETED (합계·감사필드 검증)")
    void 결제_정상흐름_WAITING에서_COMPLETED까지_합계_감사필드_검증() {
        Payment payment = newPaymentWithDetails(3000, 1000, 1000, 1000);
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        assertThat(savedPayment.getPaymentResultCode()).isEqualTo(PaymentResultCode.WAITING);
        assertThat(savedPayment.getCreatedTimestamp()).isNotNull();
        assertThat(savedPayment.getUpdatedTimestamp()).isNotNull();

        savedPayment.markRequested();
        paymentRepository.saveAndFlush(savedPayment);

        em.clear();
        Payment requestPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertThat(requestPayment.getPaymentResultCode()).isEqualTo(PaymentResultCode.REQUESTED);
        assertThat(requestPayment.getRequestedTimestamp()).isNotNull();

        requestPayment.markCompleted(TID_1, LocalDateTime.now());
        paymentRepository.saveAndFlush(requestPayment);

        em.clear();
        Payment completePayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertThat(completePayment.getPaymentResultCode()).isEqualTo(PaymentResultCode.COMPLETED);
        assertThat(completePayment.getApprovedTimestamp()).isNotNull();
        assertThat(completePayment.getExternalPaymentId()).isEqualTo(TID_1);
        assertThat(completePayment.getDetails()).hasSize(3);
    }

    @Test
    @DisplayName("합계 불일치: REQUESTED에서 COMPLETED 시도 → 예외, 상태는 REQUESTED 유지(롤백)")
    void 합계불일치_완료_예외_상태유지() {
        Payment payment = newPaymentWithDetails(3000, 1000, 1000); // 합계 불일치(2000)
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        savedPayment.markRequested();
        paymentRepository.saveAndFlush(savedPayment);

        assertThatThrownBy(() -> {
            savedPayment.markCompleted(TID_1, LocalDateTime.now());
            paymentRepository.saveAndFlush(savedPayment);
        }).isInstanceOf(PaymentStateTransitionException.class);

        em.clear();
        Payment reloadPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertThat(reloadPayment.getPaymentResultCode()).isEqualTo(PaymentResultCode.REQUESTED);
        assertThat(reloadPayment.getApprovedTimestamp()).isNull();
        assertThat(reloadPayment.getExternalPaymentId()).isNull();
    }

    // FIXME: COMPLETED 상태에서 REQUESTED, FAILED으로 변경은 막았는데 아래 테스트가 필요한지 검토 필요
    @Test
    @DisplayName("중복 승인 방지: COMPLETED 상태에서 다시 markCompleted 호출 시 예외 및 메타 불변")
    void 중복승인_예외_및_메타불변() {
        Payment payment = newPaymentWithDetails(3000, 1000, 1000, 1000);
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        savedPayment.markRequested();
        paymentRepository.saveAndFlush(savedPayment);

        String firstTid = TID_1;
        LocalDateTime firstApprovedAt = LocalDateTime.now().minusSeconds(1);
        savedPayment.markCompleted(firstTid, firstApprovedAt);
        paymentRepository.saveAndFlush(savedPayment);

        em.clear();
        Payment completePayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertThat(completePayment.getPaymentResultCode()).isEqualTo(PaymentResultCode.COMPLETED);
        assertThat(completePayment.getExternalPaymentId()).isEqualTo(firstTid);
        assertThat(completePayment.getApprovedTimestamp())
                .isCloseTo(firstApprovedAt, within(1, ChronoUnit.MICROS));

        String secondTid = TID_2;
        LocalDateTime secondApprovedAt = LocalDateTime.now();
        assertThatThrownBy(() -> {
            completePayment.markCompleted(secondTid, secondApprovedAt);
            paymentRepository.saveAndFlush(completePayment);
        }).isInstanceOf(PaymentStateTransitionException.class);

        em.clear();
        Payment reloadPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertThat(reloadPayment.getPaymentResultCode()).isEqualTo(PaymentResultCode.COMPLETED);
        assertThat(reloadPayment.getExternalPaymentId()).isEqualTo(firstTid);
        assertThat(reloadPayment.getApprovedTimestamp())
                .isCloseTo(firstApprovedAt, within(1, ChronoUnit.MICROS));
    }
}
