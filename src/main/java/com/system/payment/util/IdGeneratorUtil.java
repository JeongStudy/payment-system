package com.system.payment.util;

import org.springframework.stereotype.Component;

@Component
public class IdGeneratorUtil {
    public String oidGenerate() {
        return "DemoTest_" + System.currentTimeMillis();
    }

    public String timestampGenerate() {
        return String.valueOf(System.currentTimeMillis());
    }
}
