package com.system.payment.exception;

import lombok.Getter;

@Getter
public class PaymentServerInternalServerErrorException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentServerInternalServerErrorException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}