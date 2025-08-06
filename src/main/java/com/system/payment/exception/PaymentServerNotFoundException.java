package com.system.payment.exception;

import lombok.Getter;

@Getter
public class PaymentServerNotFoundException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentServerNotFoundException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}