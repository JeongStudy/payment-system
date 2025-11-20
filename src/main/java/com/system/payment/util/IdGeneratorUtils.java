package com.system.payment.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IdGeneratorUtils {
    public static String oidGenerate() {
        return "DemoTest_" + System.currentTimeMillis();
    }

    public static String UUIDGenerate() { return UUID.randomUUID().toString(); }

    public static String timestampGenerate() {
        return String.valueOf(System.currentTimeMillis());
    }
}
