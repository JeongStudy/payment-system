package com.system.payment.common.exception;

import lombok.Getter;

@Getter
public class PaymentServerBadRequestException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentServerBadRequestException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}