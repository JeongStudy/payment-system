package com.system.payment.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IdGeneratorUtil {
    public String oidGenerate() {
        return "DemoTest_" + System.currentTimeMillis();
    }

    public String UUIDGenerate() { return UUID.randomUUID().toString(); }

    public String timestampGenerate() {
        return String.valueOf(System.currentTimeMillis());
    }
}
