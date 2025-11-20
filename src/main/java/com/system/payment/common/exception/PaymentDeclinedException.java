package com.system.payment.common.exception;

import com.system.payment.common.dto.response.ErrorCode;
import lombok.Getter;

@Getter
// PG 결제 거부
public class PaymentDeclinedException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentDeclinedException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}