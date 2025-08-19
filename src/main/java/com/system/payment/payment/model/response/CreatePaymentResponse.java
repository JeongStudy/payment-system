package com.system.payment.payment.model.response;


import com.system.payment.payment.domain.Payment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentResponse {
	private String serviceOrderId;
	private Integer paymentId;
	private Integer eventId;

	public static CreatePaymentResponse from(Payment payment, Integer eventId) {
		return CreatePaymentResponse.builder()
				.serviceOrderId(payment.getReferenceRef().getReferenceId())
				.paymentId(payment.getId())
				.eventId(eventId)
				.build();
	}
}
