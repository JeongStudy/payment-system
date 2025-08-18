package com.system.payment.exception;

import lombok.Getter;

@Getter
public class PgResponseParseException extends RuntimeException {
    private final ErrorCode errorCode;

    public PgResponseParseException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
