package com.system.payment.common.exception;

import com.system.payment.common.dto.response.ErrorCode;
import lombok.Getter;

@Getter
public class PaymentServerNotFoundException extends RuntimeException {
	private final ErrorCode errorCode;

	public PaymentServerNotFoundException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

}