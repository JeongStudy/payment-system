package com.system.payment.payment.model.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentResponse {
	private String serviceOrderId;
	private Integer paymentId;

}
