package com.system.payment.common.exception;

import lombok.Getter;

@Getter
public class PaymentServerConflictException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentServerConflictException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}