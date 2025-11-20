package com.system.payment.common.util;

import java.time.Instant;
import java.util.UUID;

public class KeyGeneratorUtils {

	public static String generateIdempotencyKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

	public static String generateTransactionId() {
        return UUID.randomUUID().toString() + "-" + Instant.now().toEpochMilli();
    }
}
