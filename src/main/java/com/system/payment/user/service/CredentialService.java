package com.system.payment.user.service;

import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerUnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CredentialService {

	private final PasswordEncoder passwordEncoder;

	public CredentialService(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public String hash(String plainPassword) {
		return passwordEncoder.encode(plainPassword);
	}

	public void verifyOrThrow(String plainPassword, String storedHash) {
		if (!passwordEncoder.matches(plainPassword, storedHash)) {
			throw new PaymentServerUnauthorizedException(ErrorCode.INVALID_PASSWORD);
		}
	}
}
