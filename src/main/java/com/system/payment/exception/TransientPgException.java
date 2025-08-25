package com.system.payment.exception;

import lombok.Getter;

@Getter
// PG 응답 처리 실패
public class TransientPgException extends RuntimeException {
	private final ErrorCode errorCode;

	public TransientPgException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}