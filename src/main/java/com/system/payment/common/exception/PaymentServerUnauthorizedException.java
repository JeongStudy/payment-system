package com.system.payment.common.exception;

import lombok.Getter;

@Getter
public class PaymentServerUnauthorizedException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentServerUnauthorizedException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}