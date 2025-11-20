package com.system.payment.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ErrorCode {
    BAD_REQUEST_PARAM(4000, "Bad Request Parameter"),
    NOT_FOUND(4000, "Not Found"),
    UNAUTHORIZED(4000, "Unauthorized"),
    SERVER_ERROR(5000, "Server Error"),
    SERVER_NULL_POINTER_ERROR(5001, "Server Null Pointer Error"),
    RSA_KEY_PAIR_GENERATION_FAIL(5002, "An internal server error occurred during RSA key pair generation."),
    FAILED_TO_DECRYPT_AES_KEY_WITH_RSA_KEY(5003, "Failed to decrypt AES key with RSA Key."),
    FAILED_TO_ENCRYPT_AES_KEY_WITH_RSA_KEY(5004, "Failed to encrypt AES key with RSA Key."),
    FAILED_TO_DECRYPT_PASSWORD_WITH_AES_KEY(5005, "Failed to decrypt password with AES key."),
    FAILED_TO_ENCRYPT_PASSWORD_WITH_AES_KEY(5006, "Failed to encrypt password with AES key."),


    USER_NOT_EXIST(1001, "사용자가 존재하지 않습니다."),
    RSA_KEY_NOT_FOUND(1002, "RSA 키가 존재하지 않습니다."),
    INVALID_RSA_KEY(1002, "RSA 키가 유효하지 않거나 만료되었습니다."),
    AES_KEY_NOT_FOUND(1002, "AES 키가 존재하지 않습니다."),
    INVALID_AES_KEY(1003, "AES 키가 유효하지 않거나 만료되었습니다."),
    DUPLICATE_EMAIL(1004, "이미 존재하는 이메일입니다."),
    USER_ID_NOT_EXIST(1005, "사용자 ID가 존재하지 않습니다."),
    INVALID_PASSWORD(1006, "ID나 비밀번호가 틀렸습니다."),
    PG_RESPONSE_PARSE_ERROR(5000, "PG 응답 파싱에 실패했습니다."),
    DUPLICATE_PAYMENT_IDEMPOTENCY_KEY(1007, "이미 존재하는 결제 요청 정보입니다."),
    PAYMENT_ITEMS_NOT_FOUND(1008, "결제 아이템이 비어 있습니다."),
    PAYMENT_INVALID_ITEM_SUM_AMOUNT_AND_TOTAL_AMOUNT(1009, "아이템 합계와 요청 금액이 일치하지 않습니다."),
    PAYMENT_TOTAL_AMOUNT_MUST_BE_POSITIVE(1010, "결제 총액은 0보다 커야 합니다."),
    PAYMENT_VALIDATION_MISSING_FIELD(1011, "결제 요청 메시지에 필수 값이 누락되었거나 잘못되었습니다."),
    PG_TIMEOUT(1012, "PG 타임아웃 오류입니다."),
    PG_CONN_RESET(1013, "PG 연결이 리셋되었습니다."),
    PG_HTTP_503(1014, "PG 서버가 유효하지 않습니다."),
    PG_JSON_ERROR(1015, " PG 응답 파싱 오류가 발생하였습니다."),
    PAYMENT_STATE_INVALID(1016, "결제 상태 전이가 허용되지 않습니다."),
    PAYMENT_STATE_INVALID_AMOUNT(1017, "결제 금액 합계 검증에 실패했습니다.")
    ;

    private final int status;
    private final String message;

}