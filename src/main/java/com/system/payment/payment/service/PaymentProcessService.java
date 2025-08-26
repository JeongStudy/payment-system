package com.system.payment.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerNotFoundException;
import com.system.payment.exception.PaymentValidationException;
import com.system.payment.payment.domain.Payment;
import com.system.payment.payment.domain.PaymentDetail;
import com.system.payment.payment.domain.PaymentHistory;
import com.system.payment.payment.domain.PaymentResultCode;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.InicisBillingApproveResponse;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import com.system.payment.payment.repository.PaymentHistoryRepository;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessService {


    private final InicisPgClientService inicisPgClientService;      // 시뮬레이터/PG 퍼사드
    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final StringUtil stringUtil;

    /**
     * 결제 승인 프로세스
     *
     * 흐름
     *  0) 메시지 검증/추출 (approval, paymentId, txId)
     *  1) Payment 로드 (필요 시 행락/낙관적락 고려)
     *     - 이미 COMPLETED/FAILED면 멱등성 차원에서 스킵
     *  2) 상태 전이: WAITING -> REQUESTED (최초 요청 흔적)
     *  3) PG 승인 호출
     *     - 정상 응답:
     *         - res.isSuccess()==true  → [성공 확정] Detail: COMPLETED, Payment: COMPLETED(tid, approvedAt)
     *         - res.isSuccess()==false → [비즈 실패 확정] Detail: FAILED,    Payment: FAILED(resultCode, msg)
     *       두 경우 모두 History 적재 후 정상 종료(커밋) → 컨슈머 오프셋 커밋(재시도 없음)
     *     - 인프라/통신 오류(타임아웃/5xx/파싱 등) → 예외 전파
     *       → @Transactional 롤백 → 컨슈머 재시도/ DLQ 경로
     *       → (옵션) saveHistoryNewTx(REQUIRES_NEW)로 오류 히스토리 남기기
     *
     * 설계 원칙
     *  - 비즈니스 실패는 "확정 커밋": 재시도해도 성공으로 바뀌지 않으므로 예외를 던지지 않음
     *  - 인프라/통신 오류만 재시도 대상: 예외를 던져 컨슈머 재시도/ DLQ 정책에 위임
     *  - History는 changedBy/reason을 enum으로 표준화하고, 외부 응답/예외는 externalResponse(JSONB)에 기록
     *  - 멱등성: 최종 상태 스킵 + (권장) transactionId unique 인덱스 + exists 체크
     *  - 관측성: txId/traceId/MID/코드/레イ턴시 메트릭/로그/알람
     */
    @Transactional
    public void process(PaymentRequestedMessageV1<InicisBillingApproval> message) {
        // 0) 메시지 검증
        InicisBillingApproval approval = Optional.ofNullable(message)
                .map(PaymentRequestedMessageV1::payload)
                .map(PaymentRequestedMessageV1.Payload::external)
                .map(PaymentRequestedMessageV1.Payload.External::approval)
                .orElseThrow(() -> new PaymentValidationException(ErrorCode.PAYMENT_VALIDATION_MISSING_FIELD));

        final Integer paymentId = message.identifiers().paymentId();
        final String txId = message.identifiers().transactionId();

        // 1) 결제 로드
        // TODO: 비관적 락, 2-단계 패턴 고민중
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentValidationException(ErrorCode.PAYMENT_ITEMS_NOT_FOUND));

        // 멱등성/중복 처리 가드
        if (payment.getPaymentResultCode() == PaymentResultCode.COMPLETED
                || payment.getPaymentResultCode() == PaymentResultCode.FAILED) {
            log.info("[PROCESS][SKIP-FINALIZED] paymentId={}, status={}", paymentId, payment.getPaymentResultCode());
            return;
        }

        final String prevDataJson = stringUtil.toJsonSafe(payment);
        final PaymentResultCode prevCode = payment.getPaymentResultCode();

        // 2) WAITING -> REQUESTED (최초 요청 흔적)
        if (prevCode == PaymentResultCode.WAITING) {
            payment.changeResultCodeRequested();  // 상태 + requestedTimestamp 세팅
        }

        // 3) PG 승인 호출
        InicisBillingApproveResponse res = inicisPgClientService.approve(approval);

        // 4) 응답 분기
        if (!res.isSuccess()) {
            // 비즈니스 실패(거절/한도초과/잔액부족 등) → 확정 커밋(예외 던지지 않음)
            for (PaymentDetail d : payment.getDetails()) {
                d.markFailed();
            }
            payment.markFailed(res.resultCode(), res.resultMsg(), LocalDateTime.now());

            saveHistory( // 같은 트랜잭션으로도 충분하면 이 메서드 사용
                    payment, prevCode, PaymentResultCode.FAILED,
                    "KAFKA_CONSUMER", "INICIS_BILLING_APPROVE_FAIL: " + res.resultMsg(),
                    prevDataJson, res, txId
            );

            log.warn("[PROCESS][PG-BIZ-FAIL] paymentId={}, txId={}, code={}, msg={}",
                    paymentId, txId, res.resultCode(), res.resultMsg());

            // 정상 종료(커밋) → 컨슈머 오프셋 커밋 → 재시도/중복 처리 방지
            return;
        }

        // 5) 성공 처리
        for (PaymentDetail d : payment.getDetails()) {
            d.markCompleted();
        }
        payment.markCompleted(res.tid(), res.approvedAt());

        saveHistory(
                payment, prevCode, PaymentResultCode.COMPLETED,
                "KAFKA_CONSUMER", "INICIS_BILLING_APPROVE_OK",
                prevDataJson, res, txId
        );

        log.info("[PROCESS][PG-OK] paymentId={}, txId={}, tid={}, approvedAt={}",
                paymentId, txId, res.tid(), res.approvedAt());
    }

    private void saveHistory(Payment payment,
                                PaymentResultCode prevCode,
                                PaymentResultCode newCode,
                                // TODO: changedBy를 Enum으로 관리하는 것 검토
                                String changedBy,
                                // TODO: reason을 res.resultMsg()를 같이 사용할 지 검토
                                String reason,
                                String prevDataJson,
                                Object externalResponse,
                                String txId) {
        String newDataJson = stringUtil.toJsonSafe(payment);
        String externalJson = stringUtil.toJsonSafe(externalResponse);

        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .prevResultCode(prevCode != null ? prevCode.getCode() : null)
                .newResultCode(newCode.getCode())
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .changedReason(reason)
                .prevData(prevDataJson)     // JSONB
                .newData(newDataJson)       // JSONB
                .externalResponse(externalJson) // JSONB
                .transactionId(txId)
                .build();

        paymentHistoryRepository.save(history);
    }
}
