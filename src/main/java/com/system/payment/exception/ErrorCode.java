package com.system.payment.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ErrorCode {
    BAD_REQUEST_PARAM(4000, "Bad Request Parameter"),
    SERVER_ERROR(5000, "Server Error"),
    SERVER_NULL_POINTER_ERROR(5001, "Null Pointer Error"),
    RSA_KEY_GENERATION_FAIL(5002, "An internal server error occurred during RSA key generation."),
    FAILED_TO_DECRYPT_AES_KEY_WITH_RSA_KEY(5003, "Failed to decrypt AES key with RSA Key."),
    FAILED_TO_ENCRYPT_AES_KEY_WITH_RSA_KEY(5003, "Failed to encrypt AES key with RSA Key."),
    FAILED_TO_DECRYPT_PASSWORD_WITH_AES_KEY(5003, "Failed to decrypt password with AES key."),
    FAILED_TO_ENCRYPT_PASSWORD_WITH_AES_KEY(5003, "Failed to encrypt password with AES key."),


    USER_NOT_EXIST(1001, "사용자가 존재하지 않습니다."),
    RSA_KEY_NOT_FOUND(1002, "RSA 키가 존재하지 않습니다."),
    INVALID_RSA_KEY(1002, "RSA 키가 유효하지 않거나 만료되었습니다."),
    AES_KEY_NOT_FOUND(1002, "AES 키가 존재하지 않습니다."),
    INVALID_AES_KEY(1003, "AES 키가 유효하지 않거나 만료되었습니다."),
    DUPLICATE_EMAIL(1004, "이미 존재하는 이메일입니다.");

    private final int status;
    private final String message;

}