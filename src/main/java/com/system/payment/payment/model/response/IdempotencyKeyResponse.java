package com.system.payment.payment.model.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IdempotencyKeyResponse {
	private String idempotencyKey;

	public static IdempotencyKeyResponse from(String idempotencyKey) {
		return IdempotencyKeyResponse.builder()
				.idempotencyKey(idempotencyKey)
				.build();
	}
}
