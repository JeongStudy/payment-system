package com.system.payment.user.model.reponse;

import com.system.payment.user.domain.PaymentUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserResponse {
	private String email;
	private String firstName;
	private String lastName;
	private String phoneNumber;

	public static UserResponse from(PaymentUser user) {
		return UserResponse.builder()
				.email(user.getEmail())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.phoneNumber(user.getPhoneNumber())
				.build();
	}
}
