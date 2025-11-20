package com.system.payment.user.service;

import com.system.payment.common.dto.response.ErrorCode;
import com.system.payment.common.exception.PaymentServerConflictException;
import com.system.payment.user.domain.AesKey;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.model.reponse.LoginResponse;
import com.system.payment.user.model.request.LoginRequest;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.common.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final PaymentUserRepository paymentUserRepository;
	private final CryptoService cryptoService;
	private final CredentialService credentialService;
	private final JwtUtils jwtUtils;

	@Transactional
	public void signUp(SignUpRequest request) {
		if (paymentUserRepository.existsByEmail(request.getEmail())) {
			throw new PaymentServerConflictException(ErrorCode.DUPLICATE_EMAIL);
		}
		final AesKey aesKey = cryptoService.resolveValidAesKey(request.getRsaPublicKey(), request.getEncAesKey());

		final String decryptedPassword = cryptoService.decryptPasswordWithAes(request.getEncPassword(), aesKey.getAesKey());
		final String hashedPassword = credentialService.hash(decryptedPassword);

		PaymentUser user = PaymentUser.create(request.getEmail(), hashedPassword, request.getFirstName(), request.getLastName(), request.getPhoneNumber());
		paymentUserRepository.save(user);
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		PaymentUser user = paymentUserRepository.getByEmailOrThrow(request.getEmail());
		final AesKey aesKey = cryptoService.resolveValidAesKey(request.getRsaPublicKey(), request.getEncAesKey());

		final String decryptedPassword = cryptoService.decryptPasswordWithAes(request.getEncPassword(), aesKey.getAesKey());
		credentialService.verifyOrThrow(decryptedPassword, user.getEncPassword());

		final String jwtToken = jwtUtils.generateToken(user.getId());
		return LoginResponse.from(jwtToken);
	}
}