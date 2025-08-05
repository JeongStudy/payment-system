package com.system.payment.user.service;

import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerConflictException;
import com.system.payment.exception.PaymentServerNotFoundException;
import com.system.payment.exception.PaymentServerUnauthorizedException;
import com.system.payment.user.domain.jaebin.AesKey;
import com.system.payment.user.domain.jaebin.PaymentUser;
import com.system.payment.user.domain.jaebin.RsaKeyPair;
import com.system.payment.user.model.reponse.LoginResponse;
import com.system.payment.user.model.request.LoginRequest;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.user.repository.AesKeyRepository;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.user.repository.RsaKeyPairRepository;
import com.system.payment.util.AesKeyCryptoUtil;
import com.system.payment.util.JwtUtil;
import com.system.payment.util.RsaKeyCryptoUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final PaymentUserRepository paymentUserRepository;
	private final RsaKeyPairRepository rsaKeyPairRepository;
	private final AesKeyRepository aesKeyRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	public AuthService(PaymentUserRepository paymentUserRepository,
					   RsaKeyPairRepository rsaKeyPairRepository,
					   AesKeyRepository aesKeyRepository,
					   PasswordEncoder passwordEncoder,
					   JwtUtil jwtUtil) {
		this.paymentUserRepository = paymentUserRepository;
		this.rsaKeyPairRepository = rsaKeyPairRepository;
		this.aesKeyRepository = aesKeyRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
	}

	@Transactional
	public void signUp(SignUpRequest request) {
		final RsaKeyPair rsaKeyPair = rsaKeyPairRepository.findByPublicKey(request.getPublicKey())
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.RSA_KEY_NOT_FOUND));
		rsaKeyPair.validateNotExpired();

		final String decryptedAesKey = RsaKeyCryptoUtil
				.decryptEncryptedAesKeyWithRsaPrivateKey(request.getEncAesKey(), rsaKeyPair.getPrivateKey());
		final AesKey aesKey = aesKeyRepository.findByAesKey(decryptedAesKey)
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.AES_KEY_NOT_FOUND));
		aesKey.validateNotExpired();

		final String decryptedPassword = AesKeyCryptoUtil.decryptPasswordWithAesKey(request.getEncPassword(), aesKey.getAesKey());
		final String hashedPassword = passwordEncoder.encode(decryptedPassword);

		if (paymentUserRepository.existsByEmail(request.getEmail())) {
			throw new PaymentServerConflictException(ErrorCode.DUPLICATE_EMAIL);
		}

		PaymentUser user = PaymentUser.create(request.getEmail(), hashedPassword, request.getFirstName(), request.getLastName(), request.getPhoneNumber());

		paymentUserRepository.save(user);
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {

		PaymentUser user = paymentUserRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.USER_NOT_EXIST));

		RsaKeyPair rsaKeyPair = rsaKeyPairRepository.findByPublicKey(request.getRsaPublicKey())
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.RSA_KEY_NOT_FOUND));

		rsaKeyPair.validateNotExpired();

		String aesKey = RsaKeyCryptoUtil.decryptEncryptedAesKeyWithRsaPrivateKey(request.getEncAesKey(), rsaKeyPair.getPrivateKey());

		AesKey aesKeyEntity = aesKeyRepository.findByAesKey(aesKey)
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.AES_KEY_NOT_FOUND));

		aesKeyEntity.validateNotExpired();

		String plainPassword = AesKeyCryptoUtil.decryptPasswordWithAesKey(request.getPassword(), aesKey);

		if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
			throw new PaymentServerUnauthorizedException(ErrorCode.INVALID_PASSWORD);
		}

		String jwtToken = jwtUtil.generateToken(user.getId());

		return new LoginResponse(jwtToken);
	}
}