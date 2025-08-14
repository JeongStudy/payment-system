package com.system.payment.payment.model.response;


import com.system.payment.payment.domain.Payment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentResponse {
	private String serviceOrderId;
	private Integer paymentId;

	public static CreatePaymentResponse from(Payment payment) {
		return CreatePaymentResponse.builder()
				.serviceOrderId(payment.getReferenceRef().getReferenceId())
				.paymentId(payment.getId())
				.build();
	}
}
