package com.system.payment.payment.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

	@NotNull
	private Integer paymentUserCardId;

	@NotNull
	private String serviceOrderId;

	private Integer pointId;

	private Integer couponId;

	@NotBlank
	private String productName;

	@NotNull
	private Integer amount;

	@NotNull
	private String idempotencyKey;

	// 결제 인증
	@NotBlank
	private String encPassword;

	@NotBlank
	private String encAesKey;

	@NotBlank
	private String rsaPublicKey;
}
