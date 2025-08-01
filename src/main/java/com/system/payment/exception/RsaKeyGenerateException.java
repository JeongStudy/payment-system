package com.system.payment.exception;

import lombok.Getter;

@Getter
public class RsaKeyGenerateException extends RuntimeException {
    private final ErrorCode errorCode;

	public RsaKeyGenerateException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}
}