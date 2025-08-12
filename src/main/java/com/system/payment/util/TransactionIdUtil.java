package com.system.payment.util;

import java.time.Instant;
import java.util.UUID;

public class TransactionIdUtil {
	public static String generate() {
        return UUID.randomUUID().toString() + "-" + Instant.now().toEpochMilli();
    }
}
