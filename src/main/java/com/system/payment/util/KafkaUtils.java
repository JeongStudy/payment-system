package com.system.payment.util;

import com.system.payment.common.dto.response.ErrorCode;
import com.system.payment.common.exception.PaymentValidationException;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class KafkaUtils {

    public static String extractKafkaHeader(Map<String, Object> headers, String key) {
        Object v = headers.get(key);
        if (v == null) return null;
        if (v instanceof byte[] arr) return new String(arr, StandardCharsets.UTF_8);
        return String.valueOf(v);
    }

    public static void validateMessagePayload(
            String idempotencyKey,
            String txId,
            PaymentRequestedMessageV1.Payload.External<InicisBillingApproval> external,
            InicisBillingApproval approval
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || txId == null || txId.isBlank()
                || external == null
                || external.provider() == null
                || approval == null
                || approval.getMid() == null || approval.getMid().isBlank()) {

            log.warn("[VALIDATION] invalid payload: idempotencyKey={}, txId={}, external={}, provider={}, approval={}, mid={}",
                    StringUtils.safe(idempotencyKey), StringUtils.safe(txId),
                    external != null, (external != null ? external.provider() : null),
                    (approval != null), (approval != null ? approval.getMid() : null));

            throw new PaymentValidationException(ErrorCode.PAYMENT_VALIDATION_MISSING_FIELD);
        }
        // 필요 시 추가 필드:
        // if (approval.getOid() == null || approval.getOid().isBlank()) throw new IllegalArgumentException("approval.oid required");
        // if (approval.getTid() == null || approval.getTid().isBlank()) throw new IllegalArgumentException("approval.tid required");
    }
}
