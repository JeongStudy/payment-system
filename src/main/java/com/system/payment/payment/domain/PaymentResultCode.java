package com.system.payment.payment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PaymentResultCode {
    WAITING("00", "결제 대기"),
    REQUESTED("11", "결제 요청"),
    COMPLETED("22", "결제 완료"),
    FAILED("33", "결제 실패"),
    CANCELED("44", "결제 취소");

    private final String code;
    private final String description;

    public static PaymentResultCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(c -> c.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment result code: " + code));
    }
}