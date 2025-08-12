package com.system.payment.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum SuccessCode {
    OK(2000, "요청이 정상적으로 처리되었습니다."),
    USER_CREATED(2001, "회원가입이 완료되었습니다."),
    RAS_KEY_CREATED(2002, "RSA Key가 생성되었습니다."),
    AES_KEY_CREATED(2003, "AES Key가 생성되었습니다."),
    USER_LOGIN_SUCCESS(2004, "로그인이 완료되었습니다."),
    PAYMENT_REQUESTS_SUCCESS(2005, "결제 요청이 완료되었습니다."),
    ;

    private final int status;
    private final String message;

}