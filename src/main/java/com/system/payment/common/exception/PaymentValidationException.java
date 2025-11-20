package com.system.payment.common.exception;

import com.system.payment.common.dto.response.ErrorCode;
import lombok.Getter;

@Getter
// Kafka 메시지 유효성 검사 실패
public class PaymentValidationException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentValidationException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}