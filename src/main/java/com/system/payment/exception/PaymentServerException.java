package com.system.payment.exception;

import lombok.Getter;

@Getter
public class PaymentServerException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentServerException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}