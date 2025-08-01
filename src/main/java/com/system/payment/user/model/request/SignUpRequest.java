package com.system.payment.user.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
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