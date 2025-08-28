package com.system.payment.payment.service;

import com.system.payment.exception.ErrorCode;

public enum PgSimErrorTrigger {
    TIMEOUT(ErrorCode.PG_TIMEOUT),
    CONN_RESET(ErrorCode.PG_CONN_RESET),
    HTTP_503(ErrorCode.PG_HTTP_503),
    JSON_ERROR(ErrorCode.PG_JSON_ERROR);

    public final ErrorCode code;

    PgSimErrorTrigger(ErrorCode code) { this.code = code; }
}

