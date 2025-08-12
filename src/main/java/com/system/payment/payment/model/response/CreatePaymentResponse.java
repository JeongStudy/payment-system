package com.system.payment.payment.model.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentResponse {
	private Integer oid;
	private Integer pid;

}
