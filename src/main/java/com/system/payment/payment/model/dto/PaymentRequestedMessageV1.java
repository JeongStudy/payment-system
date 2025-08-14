package com.system.payment.payment.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * PaymentRequestedMessageV1
 * - Envelope(메타) + Identifiers(식별자) + Payload(도메인 데이터)
 * - traceId/traceparent 등 관측성 메타는 Kafka Header로만 전파(바디엔 넣지 않음)
 * - 제네릭 A: 외부 PG 승인 페이로드 타입 (예: InicisBillingApproval)
 */
public record PaymentRequestedMessageV1<A>(
        Envelope envelope,
        Identifiers identifiers,
        Payload<A> payload
) {
    /** 메시지 스키마 버전/토픽/헤더 키 상수 */
    public static final String SCHEMA_VERSION = "v1";
    public static final String TOPIC = "payment.requested.v1";

    /** 권장 헤더 키(예시) */
    public static final class Headers {
        public static final String VERSION = "version";
        public static final String IDEMPOTENCY_KEY = "idempotencyKey";
        public static final String PRODUCER = "producer";
        public static final String TRACEPARENT = "traceparent"; // traceId/spanId 전파용(W3C)
        private Headers() {}
    }

    /** 최소 생성 팩토리: Envelope 생성 편의 메서드 포함 */
    public static <A> PaymentRequestedMessageV1<A> of(
            String producer,
            String idempotencyKey,
            Identifiers identifiers,
            Payload<A> payload
    ) {
        return new PaymentRequestedMessageV1<>(
                Envelope.newEnvelope(producer, idempotencyKey),
                identifiers,
                payload
        );
    }

    /** 바디 전체에 대한 간단 검증 */
    public PaymentRequestedMessageV1 {
        Objects.requireNonNull(envelope, "envelope must not be null");
        Objects.requireNonNull(identifiers, "identifiers must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    // ───────────────────────── Envelope ─────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record Envelope(
            /** 메시지(전송물) 고유 ID, de-dupe 기준 */
            @JsonProperty("messageId") String messageId,
            /** 스키마 버전(예: "v1") */
            @JsonProperty("version") String version,
            /** 이벤트 발생 시각(UTC, ISO-8601) */
            @JsonProperty("occurredAt") Instant occurredAt,
            /** 발행 서비스/모듈 명(예: "payment-api") */
            @JsonProperty("producer") String producer,
            /** 요청 멱등 키(동일 요청 중복 방지) */
            @JsonProperty("idempotencyKey") String idempotencyKey
    ) {
        /** 기본 Envelope 팩토리: messageId/occurredAt 자동 생성 */
        public static Envelope newEnvelope(String producer, String idempotencyKey) {
            return new Envelope(
                    UUID.randomUUID().toString(),
                    SCHEMA_VERSION,
                    Instant.now(),  // UTC
                    producer,
                    idempotencyKey
            );
        }

        public Envelope {
            if (messageId == null || messageId.isBlank()) {
                throw new IllegalArgumentException("messageId must not be blank");
            }
            if (version == null || version.isBlank()) {
                throw new IllegalArgumentException("version must not be blank");
            }
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            if (producer == null || producer.isBlank()) {
                throw new IllegalArgumentException("producer must not be blank");
            }
            // idempotencyKey는 정책에 따라 null 허용 가능 -> 강제하지 않음
        }
    }

    // ──────────────────────── Identifiers ───────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record Identifiers(
            /** 표준 상관키이자 Kafka 파티션 키로 사용 */
            @JsonProperty("paymentId") Integer paymentId,
            /** 내부 비즈니스 트랜잭션 ID(외부 PG TID와 구분) */
            @JsonProperty("transactionId") String transactionId,
            /** 내부 사용자 식별자 */
            @JsonProperty("userId") Integer userId
    ) {
        public Identifiers {
            Objects.requireNonNull(paymentId, "paymentId must not be null");
            // transactionId/userId는 케이스에 따라 null 허용
        }

        /** Kafka 파티션 키로 사용할 값 */
        public String partitionKey() {
            return String.valueOf(paymentId);
        }
    }

    // ───────────────────────── Payload ──────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record Payload<T>(
            @JsonProperty("payment") PaymentSnapshot payment,
            @JsonProperty("external") External<T> external
    ) {
        public Payload {
            Objects.requireNonNull(payment, "payment must not be null");
            // external은 케이스에 따라 null 허용(외부 승인 단계가 나중일 수도 있음)
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static record PaymentSnapshot(
                /** 결제 금액(양수) */
                @JsonProperty("amount") Integer amount,
                /** 통화(예: "KRW") */
                @JsonProperty("currency") String currency,
                /** 결제수단(예: "CARD", "ACCOUNT") */
                @JsonProperty("methodType") String methodType,
                /** 내부 카드 참조(카드 결제 시) */
                @JsonProperty("cardId") Integer cardId,
                /** 상태(예: "00") */
                @JsonProperty("paymentResultCode") String paymentResultCode,
                /** 요청 시각(UTC), payment 만든 시각 */
                @JsonProperty("requestedAt") Instant requestedAt
        ) {
            public PaymentSnapshot {
                if (amount == null || amount <= 0) {
                    throw new IllegalArgumentException("amount must be positive");
                }
                if (currency == null || currency.isBlank()) {
                    throw new IllegalArgumentException("currency must not be blank");
                }
                if (methodType == null || methodType.isBlank()) {
                    throw new IllegalArgumentException("methodType must not be blank");
                }
                Objects.requireNonNull(requestedAt, "requestedAt must not be null");
                // cardId/status는 도메인 정책에 따라 null 허용 가능
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static record External<T>(
                /** PG 제공자 식별(예: "INICIS") */
                @JsonProperty("provider") String provider,
                /**
                 * 외부 PG 승인 페이로드(예: InicisBillingApproval)
                 * - 제네릭 T로 받아 타입 안전하게 캡슐화
                 */
                @JsonProperty("approval") T approval
        ) {
            public External {
                if (provider != null && provider.isBlank()) {
                    throw new IllegalArgumentException("provider must not be blank when present");
                }
                // approval/pgTid는 단계에 따라 null 허용
            }
        }
    }
}
