package com.system.payment.user.model.reponse;

import com.system.payment.user.domain.AesKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AesKeyResponse {
	private String aesKey;

	public static AesKeyResponse from(AesKey aesKey) {
		return AesKeyResponse.builder()
				.aesKey(aesKey.getAesKey())
				.build();
	}
}