package com.system.payment.common.exception;

import lombok.Getter;

@Getter
public class PaymentServerInternalServerErrorException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentServerInternalServerErrorException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}