package com.system.payment.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ErrorCode {
    BAD_REQUEST_PARAM(4000, "Bad Request Parameter"),
    SERVER_NULL_POINTER_ERROR(5000, "Null Pointer Error"),
    SERVER_ERROR(5000, "Server Error"),

    USER_NOT_EXIST(1001, "사용자가 존재하지 않습니다.");

    private final int status;
    private final String message;

}