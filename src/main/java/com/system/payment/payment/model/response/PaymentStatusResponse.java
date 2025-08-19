package com.system.payment.payment.model.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentStatusResponse {
	private Integer paymentId;
	private String code;
	private String description;
}