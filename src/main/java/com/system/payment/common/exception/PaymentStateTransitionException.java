package com.system.payment.common.exception;

import lombok.Getter;

@Getter
// PG 결제 거부
public class PaymentStateTransitionException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentStateTransitionException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}