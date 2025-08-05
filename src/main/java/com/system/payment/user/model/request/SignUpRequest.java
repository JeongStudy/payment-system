package com.system.payment.user.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

	@NotBlank
	private String publicKey;

	@NotBlank
	private String encAesKey;

	@Email
	@NotBlank
	private String email;

	@NotBlank
	private String encPassword;

	@NotBlank
	private String firstName;

	@NotBlank
	private String lastName;

	@NotBlank
	private String phoneNumber;

}