package com.system.payment.util;

import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KafkaUtil {

    public String extractKafkaHeader(Map<String, Object> headers, String key) {
        Object v = headers.get(key);
        if (v == null) return null;
        if (v instanceof byte[] arr) return new String(arr, StandardCharsets.UTF_8);
        return String.valueOf(v);
    }

    public void validateMessagePayload(
            String idempotencyKey,
            String txId,
            PaymentRequestedMessageV1.Payload.External<InicisBillingApproval> external,
            InicisBillingApproval approval
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) { throw new IllegalArgumentException("idempotencyKey required");}
        if (txId == null || txId.isBlank()) { throw new IllegalArgumentException("transactionId required");}
        if (external == null) { throw new IllegalArgumentException("payload.external required"); }
        if (external.provider() == null) { throw new IllegalArgumentException("external.provider required"); }
        if (approval == null) { throw new IllegalArgumentException("external.approval required"); }
        if (approval.getMid() == null || approval.getMid().isBlank()) { throw new IllegalArgumentException("approval.mid required"); }
        // 필요 시 추가 필드:
        // if (approval.getOid() == null || approval.getOid().isBlank()) throw new IllegalArgumentException("approval.oid required");
        // if (approval.getTid() == null || approval.getTid().isBlank()) throw new IllegalArgumentException("approval.tid required");
    }
}
