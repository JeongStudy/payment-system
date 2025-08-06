package com.system.payment.user.service;

import com.system.payment.exception.CryptoException;
import com.system.payment.exception.ErrorCode;
import com.system.payment.user.domain.AesKey;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.domain.RsaKeyPair;
import com.system.payment.user.model.reponse.LoginResponse;
import com.system.payment.user.model.request.LoginRequest;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.user.repository.AesKeyRepository;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.user.repository.RsaKeyPairRepository;
import com.system.payment.util.AesKeyCryptoUtil;
import com.system.payment.util.JwtUtil;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
		String encPassword = AesKeyCryptoUtil.encryptPasswordWithAesKey(password, aesKeyStr);

		// 회원 가입 요청 객체 생성
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

		// AES 대칭키 복호화 확인
		String decryptedAesKey = RsaKeyCryptoUtil.decryptEncryptedAesKeyWithRsaPrivateKey(encAesKey, privateKey);
		assert decryptedAesKey.equals(aesKeyStr);

		// 평문 비밀번호 확인
		String decryptedPassword = AesKeyCryptoUtil.decryptPasswordWithAesKey(encPassword, aesKeyStr);
		assert decryptedPassword.equals(password);

		// region given-when-then
		// given
		given(aesKeyRepository.findByAesKey(anyString())).willReturn(Optional.of(AesKey.create(aesKeyStr)));
		given(rsaKeyPairRepository.findByPublicKey(publicKey)).willReturn(Optional.of(RsaKeyPair.create(publicKey, privateKey)));
		given(paymentUserRepository.existsByEmail(anyString())).willReturn(false);

		// when
		// 회원가입 수행
		authService.signUp(request);

		// then
		verify(paymentUserRepository, times(1)).save(any(PaymentUser.class));
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
		String encPassword = AesKeyCryptoUtil.encryptPasswordWithAesKey(password, aesKeyStr);

		// 회원 가입 요청 객체 생성
		SignUpRequest signUpRequest = SignUpRequest.builder()
				.publicKey(publicKey)
				.encAesKey(encAesKey)
				.encPassword(encPassword)
//				.email("jaebin1291@naver.com")
				.email("jaebin1292@naver.com")
				.firstName("JAEBIN")
				.lastName("CHUNG")
				.phoneNumber("01025861111")
				.build();

		// AES 대칭키 복호화 확인
		String decryptedAesKey = RsaKeyCryptoUtil.decryptEncryptedAesKeyWithRsaPrivateKey(encAesKey, privateKey);
		assert decryptedAesKey.equals(aesKeyStr);

		// 평문 비밀번호 확인
		String decryptedPassword = AesKeyCryptoUtil.decryptPasswordWithAesKey(encPassword, aesKeyStr);
		assert decryptedPassword.equals(password);

		// region given-when-then
		// given
		given(aesKeyRepository.findByAesKey(anyString())).willReturn(Optional.of(AesKey.create(aesKeyStr)));
		given(rsaKeyPairRepository.findByPublicKey(publicKey)).willReturn(Optional.of(RsaKeyPair.create(publicKey, privateKey)));
		given(paymentUserRepository.existsByEmail(anyString())).willReturn(false);
		String bcryptHash = "bcrypt-hash";
		given(passwordEncoder.encode(anyString())).willReturn(bcryptHash);

		// when
		// 회원가입 수행
		authService.signUp(signUpRequest);

		// region 로그인 과정
		// 로그인 유저 Mock 객체 생성
		PaymentUser mockUser = PaymentUser.create(
				signUpRequest.getEmail(),
				bcryptHash,
				signUpRequest.getFirstName(),
				signUpRequest.getLastName(),
				signUpRequest.getPhoneNumber()
		);

		// login 시의 패스워드 AES 암호화 (AES 암호문)
		String encLoginPassword = AesKeyCryptoUtil.encryptPasswordWithAesKey(password, aesKeyStr);

		// login 요청 객체
		LoginRequest loginRequest = LoginRequest.builder()
				.email(signUpRequest.getEmail())
				.rsaPublicKey(publicKey)
				.encAesKey(encAesKey)
				.password(encLoginPassword)
				.build();

		// login 과정에서의 mock 리턴값 세팅
		given(paymentUserRepository.findByEmail(signUpRequest.getEmail()))
				.willReturn(Optional.of(mockUser));

		given(passwordEncoder.matches(password, bcryptHash)).willReturn(true);

		// jwt 생성 mock
		given(jwtUtil.generateToken(mockUser.getId()))
				.willReturn("jwt-token");

		// 실제 로그인 서비스 호출
		LoginResponse response = authService.login(loginRequest);

		// then
		assert response != null;
		assert response.getToken() != null;
		assert response.getToken().equals("jwt-token");
	}
}