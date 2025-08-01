package com.system.payment.user.service;

import com.system.payment.exception.CryptoException;
import com.system.payment.exception.ErrorCode;
import com.system.payment.user.domain.jaebin.AesKey;
import com.system.payment.user.domain.jaebin.PaymentUser;
import com.system.payment.user.domain.jaebin.RsaKeyPair;
import com.system.payment.user.model.reponse.AesKeyResponse;
import com.system.payment.user.model.reponse.RsaKeyResponse;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.user.repository.AesKeyRepository;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.user.repository.RsaKeyPairRepository;
import com.system.payment.util.AesKeyCryptoUtil;
import com.system.payment.util.RsaKeyCryptoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private PaymentUserRepository paymentUserRepository;

	@Mock
	private RsaKeyPairRepository rsaKeyPairRepository;

	@Mock
	private AesKeyRepository aesKeyRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private CryptoService cryptoService;

	@InjectMocks
	private AuthService authService;

	private static final Logger logger = LoggerFactory.getLogger(AuthServiceTest.class);

	@Test
	@DisplayName("회원가입 서비스 - 정상 플로우")
	void signUp_success() throws NoSuchAlgorithmException {
		// region rsa generate
		KeyPairGenerator keyGen = null;
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(ErrorCode.RSA_KEY_GENERATION_FAIL);
		}
		keyGen.initialize(2048);
		KeyPair pair = keyGen.generateKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
		String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
		// region

		// region aes generate
		String aesKeyStr;
		do {
			aesKeyStr = UUID.randomUUID().toString();
		} while (aesKeyRepository.existsByAesKey(aesKeyStr));
		AesKey aesKey = AesKey.create(aesKeyStr);
		// region

		String encAesKey = RsaKeyCryptoUtil.encryptAesKeyWithRsaPublicKey(aesKeyStr, publicKey);
		String password = "manager0";
		String encPassword = AesKeyCryptoUtil.encryptPasswordWithAesKey(password, aesKeyStr);

		SignUpRequest request = SignUpRequest.builder()
				.publicKey(publicKey)
				.encAesKey(encAesKey)
				.encPassword(encPassword)
//				.email("jaebin1291@naver.com")
				.email("jaebin1292@naver.com")
				.firstName("JAEBIN")
				.lastName("CHUNG")
				.phoneNumber("01025861111")
				.build();


		AesKey aesKeyEntity = mock(AesKey.class);
//		given(aesKeyRepository.findByAesKey(aesKeyStr)).willReturn(Optional.of(aesKeyEntity));

		String decryptedAesKey = RsaKeyCryptoUtil.decryptEncryptedAesKeyWithRsaPrivateKey(encAesKey, privateKey);
		assert decryptedAesKey.equals(aesKeyStr);

		String decryptedPassword = AesKeyCryptoUtil.decryptPasswordWithAesKey(encPassword, aesKeyStr);
		assert decryptedPassword.equals(password);

//		assertThatNoException().isThrownBy(() -> authService.signUp(request));

//		verify(paymentUserRepository).save(any(PaymentUser.class));

		logger.info("");
	}
}