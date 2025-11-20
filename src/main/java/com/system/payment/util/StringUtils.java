package com.system.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
public class StringUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String safe(String s) { return s == null ? "" : s; }

    public static String orDefault(String v, String def) { return (v == null || v.isBlank()) ? def : v; }

    public static String randomDigits(int n) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    public static String toJsonSafe(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.warn("json serialize failed: {}", e.getMessage());
            return null; // History 컬럼이 nullable이므로 null 허용
        }
    }
}
