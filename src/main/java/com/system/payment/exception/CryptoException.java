package com.system.payment.exception;

import lombok.Getter;

@Getter
public class CryptoException extends RuntimeException {
    private final ErrorCode errorCode;

	public CryptoException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}
}