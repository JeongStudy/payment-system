package com.system.payment.payment.service;

import com.system.payment.exception.PaymentValidationException;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import com.system.payment.util.IdGeneratorUtil;
import com.system.payment.util.KafkaUtil;
import com.system.payment.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentProcessService paymentProcessService;
    private final PaymentIdempotencyGuard idempotencyGuard;

    public static final String HDR_TRACE_ID = "X-Trace-Id";
    public static final String HDR_SPAN_ID  = "X-Span-Id";
    public static final String HDR_CORR_ID  = "X-Correlation-Id";

    // MDC 키
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID  = "spanId";
    private static final String MDC_CORR_ID  = "corrId";
    private static final String MDC_TX_ID    = "txId";
    private static final String MDC_IDEMPOTENCY_KEY = "idempotencyKey";

    @KafkaListener(
            topics = "payment.requested.v1",
            groupId = "payment-request-consumer",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void onMessage(
            PaymentRequestedMessageV1<InicisBillingApproval> msg,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Headers Map<String, Object> headers
    ) {

        // TODO: 헤더/트레이싱/밸리데이션/로그
        // TODO: 멱등성/중복처리 가드
        // TODO: 서비스 위임

        // 1) 헤더 추출(null/byte[] 안전)
        String traceId = KafkaUtil.extractKafkaHeader(headers, HDR_TRACE_ID);
        String spanId  = KafkaUtil.extractKafkaHeader(headers, HDR_SPAN_ID);
        String corrId  = KafkaUtil.extractKafkaHeader(headers, HDR_CORR_ID);

        // 1-1) 기본값 보정
        traceId = StringUtil.orDefault(traceId, IdGeneratorUtil.UUIDGenerate()); // 없으면 새 UUID 생성
        spanId  = StringUtil.orDefault(spanId,  "span-" + IdGeneratorUtil.UUIDGenerate().substring(0, 8));
        corrId  = StringUtil.orDefault(corrId,  "corr-" + IdGeneratorUtil.UUIDGenerate().substring(0, 8));

        // 2) 메시지 안전 파싱(null 안전)
        String idempotencyKey = Optional.ofNullable(msg).map(PaymentRequestedMessageV1::envelope)
                .map(PaymentRequestedMessageV1.Envelope::idempotencyKey).orElse(null);
        String txId = Optional.ofNullable(msg).map(PaymentRequestedMessageV1::identifiers)
                .map(PaymentRequestedMessageV1.Identifiers::transactionId).orElse(null);

        PaymentRequestedMessageV1.Payload<InicisBillingApproval>
                payload = msg != null ? msg.payload() : null;
        PaymentRequestedMessageV1.Payload.External<InicisBillingApproval>
                external = payload != null ? payload.external() : null;
        InicisBillingApproval
                approval = external != null ? external.approval() : null;

        // 4) MDC에 넣기 (try/finally로 반드시 clear)
        try {
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_SPAN_ID,  spanId);
            MDC.put(MDC_CORR_ID,  corrId);
            if (txId != null){
                MDC.put(MDC_TX_ID, txId);
            }
            if (idempotencyKey != null) {
                MDC.put(MDC_IDEMPOTENCY_KEY, idempotencyKey);
            }

            log.info("[CONSUME] key={}, partition={}, offset={}, provider={}, mid={}",
                    StringUtil.safe(key), partition, offset,
                    external != null ? external.provider() : null,
                    approval != null ? approval.getMid() : null
            );

            // 5) 밸리데이션 (필수 필드 누락 시 재시도 무의미 → swallow)
            KafkaUtil.validateMessagePayload(idempotencyKey, txId, external, approval);

            // 6) 멱등성 가드
            if (!idempotencyGuard.tryAcquire(idempotencyKey)) {
                log.warn("[IDEMPOTENCY] duplicate ignored.");
                return;
            }

            // 7) 실제 처리
            paymentProcessService.process(msg);

            // 8) 성공 마킹
            idempotencyGuard.markSuccess(idempotencyKey);

        } catch (PaymentValidationException | IllegalArgumentException e) {
            log.error("[VALIDATION] invalid message: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[PROCESS] unexpected error", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
