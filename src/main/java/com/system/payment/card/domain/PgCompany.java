package com.system.payment.card.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PgCompany {
    INICIS("INICIS");

    private final String code;

    public static PgCompany from(String name) {
        return Arrays.stream(values())
                .filter(pg -> pg.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported PG company: " + name));
    }
}