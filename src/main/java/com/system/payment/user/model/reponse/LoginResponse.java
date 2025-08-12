package com.system.payment.user.model.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LoginResponse {
	private String token;

	public static LoginResponse from(String token) {
		return LoginResponse.builder()
				.token(token)
				.build();
	}
}
