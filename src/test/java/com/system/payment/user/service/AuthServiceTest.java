package com.system.payment.user.service;

import com.system.payment.exception.CryptoException;
import com.system.payment.exception.ErrorCode;
import com.system.payment.user.domain.AesKey;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.model.reponse.LoginResponse;
import com.system.payment.user.model.request.LoginRequest;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.user.repository.AesKeyRepository;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.user.repository.RsaKeyPairRepository;
import com.system.payment.util.AesKeyCryptoUtils;
import com.system.payment.util.JwtUtil;
import com.system.payment.util.RsaKeyCryptoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private PaymentUserRepository paymentUserRepository;

	@Mock
	private RsaKeyPairRepository rsaKeyPairRepository;

	@Mock
	private CryptoService cryptoService;
	@Mock
	private CredentialService credentialService;

	@Mock
	private AesKeyRepository aesKeyRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private AuthService authService;

	private static final Logger logger = LoggerFactory.getLogger(AuthServiceTest.class);

	@Test
	@DisplayName("회원가입 서비스 - 정상 플로우")
	void signUp_success() {
		// RSA 키 생성
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

		// AES 키 생성
		String aesKeyStr;
		do {
			aesKeyStr = UUID.randomUUID().toString();
		} while (aesKeyRepository.existsByAesKey(aesKeyStr));
		AesKey aesKey = AesKey.create(aesKeyStr);

		// AES 대칭키 RSA 공개키 암호화, 비밀번호 암호화된 AES 대칭키로 AES 암호화
		String encAesKey = RsaKeyCryptoUtil.encryptAesKeyWithRsaPublicKey(aesKeyStr, publicKey);
		String password = "manager0";
		String encPassword = AesKeyCryptoUtils.encryptPasswordWithAesKey(password, aesKeyStr);

		// 회원 가입 요청 객체 생성
		SignUpRequest request = SignUpRequest.builder()
				.rsaPublicKey(publicKey)
				.encAesKey(encAesKey)
				.encPassword(encPassword)
//				.email("jaebin1291@naver.com")
				.email("jaebin1292@naver.com")
				.firstName("JAEBIN")
				.lastName("CHUNG")
				.phoneNumber("01025861111")
				.build();

//		// AES 대칭키 복호화 확인
//		String decryptedAesKey = RsaKeyCryptoUtil.decryptEncryptedAesKeyWithRsaPrivateKey(encAesKey, privateKey);
//		assert decryptedAesKey.equals(aesKeyStr);
//
//		// 평문 비밀번호 확인
//		String decryptedPassword = AesKeyCryptoUtils.decryptPasswordWithAesKey(encPassword, aesKeyStr);
//		assert decryptedPassword.equals(password);

		// region given-when-then
		// given
		given(paymentUserRepository.existsByEmail(anyString())).willReturn(false);
		given(cryptoService.resolveValidAesKey(eq(publicKey), eq(encAesKey)))
				.willReturn(AesKey.create(aesKeyStr));
		given(cryptoService.decryptPasswordWithAes(eq(encPassword), eq(aesKeyStr)))
				.willReturn(password);
		given(credentialService.hash(eq(password)))
				.willReturn("bcrypt-hash");

		// when
		// 회원가입 수행
		authService.signUp(request);

		// then
		verify(paymentUserRepository, times(1)).save(any(PaymentUser.class));
        verify(cryptoService, times(1)).resolveValidAesKey(eq(publicKey), eq(encAesKey));
        verify(cryptoService, times(1)).decryptPasswordWithAes(eq(encPassword), eq(aesKeyStr));
        verify(credentialService, times(1)).hash(eq(password));
		//endregion

		logger.info("");
	}

	@Test
	@DisplayName("로그인 서비스 - 정상 플로우")
	void login_success() {
		// RSA 키 생성
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

		// AES 키 생성
		String aesKeyStr;
		do {
			aesKeyStr = UUID.randomUUID().toString();
		} while (aesKeyRepository.existsByAesKey(aesKeyStr));
		AesKey aesKey = AesKey.create(aesKeyStr);

		// AES 대칭키 RSA 공개키 암호화, 비밀번호 암호화된 AES 대칭키로 AES 암호화
		String encAesKey = RsaKeyCryptoUtil.encryptAesKeyWithRsaPublicKey(aesKeyStr, publicKey);
		String password = "manager0";
		String encPassword = AesKeyCryptoUtils.encryptPasswordWithAesKey(password, aesKeyStr);

        // signUp 내부에서 쓰인 Crypto/Credential 동작은 여기선 검증 대상 아님이라 생략 가능

        // 로그인용 Request
        LoginRequest loginRequest = LoginRequest.builder()
                .email("jaebin1292@naver.com")
                .rsaPublicKey(publicKey)
                .encAesKey(encAesKey)
                .encPassword(encPassword)
                .build();

        // 로그인 시 사용자 조회 Mock
        PaymentUser mockUser = PaymentUser.create(
                "jaebin1292@naver.com",
                "bcrypt-hash", // 저장된 해시
                "JAEBIN",
                "CHUNG",
                "01025861111"
        );
        given(paymentUserRepository.findByEmail(eq("jaebin1292@naver.com")))
                .willReturn(Optional.of(mockUser));

        // Crypto & Credential 동작 Mock
        given(cryptoService.resolveValidAesKey(eq(publicKey), eq(encAesKey)))
                .willReturn(AesKey.create(aesKeyStr));
        given(cryptoService.decryptPasswordWithAes(eq(encPassword), eq(aesKeyStr)))
                .willReturn(password);
        doNothing().when(credentialService).verifyOrThrow(eq(password), eq("bcrypt-hash"));

        // JWT
        given(jwtUtil.generateToken(any()))
                .willReturn("jwt-token");

        // when
        LoginResponse response = authService.login(loginRequest);

        // then
        assert response != null;
        assert response.getToken().equals("jwt-token");
        verify(cryptoService, times(1)).resolveValidAesKey(eq(publicKey), eq(encAesKey));
        verify(cryptoService, times(1)).decryptPasswordWithAes(eq(encPassword), eq(aesKeyStr));
        verify(credentialService, times(1)).verifyOrThrow(eq(password), eq("bcrypt-hash"));
        verify(jwtUtil, times(1)).generateToken(any());

	}
}