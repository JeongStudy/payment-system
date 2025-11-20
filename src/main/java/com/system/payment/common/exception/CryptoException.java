package com.system.payment.common.exception;

import com.system.payment.common.dto.response.ErrorCode;
import lombok.Getter;

@Getter
public class CryptoException extends RuntimeException {
    private final ErrorCode errorCode;

	public CryptoException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}
}