package com.system.payment.payment.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentRequest {

	@NotNull
	private final Integer paymentUserCardId;

	@NotNull
	private final Integer orderId;

	@NotBlank
	private final String productName;

	@NotNull
	private final Integer amount;

	@NotNull
	private final String idempotencyKey;

	// 결제 인증
	@NotBlank
	private final String encPassword;

	@NotBlank
	private final String encAesKey;

	@NotBlank
	private final String rsaPublicKey;
}
