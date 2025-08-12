package com.system.payment.user.model.reponse;

import com.system.payment.user.domain.RsaKeyPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RsaKeyResponse {
	private String publicKey;

	public static RsaKeyResponse from(RsaKeyPair rsaKeyPair) {
		return RsaKeyResponse.builder()
				.publicKey(rsaKeyPair.getPublicKey())
				.build();
	}
}