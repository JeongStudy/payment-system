package com.system.payment.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.payment.service.PaymentProducer;
import com.system.payment.user.model.request.LoginRequest;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.util.AesKeyCryptoUtils;
import com.system.payment.util.RsaKeyCryptoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
//@ActiveProfiles("integration")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PaymentProducer paymentProducer;  // 메시지 발행 막기

	@MockitoBean
	private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry; // Listener 자체 Mock

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Value("${sql.init-sign-up-secret-sql:}")
	String initSignUpSql;

	private static final Logger logger = LoggerFactory.getLogger(AuthControllerTest.class);

	// 전역 변수 선언
	private String email;
	private String password;
	private String firstName;
	private String lastName;
	private String phoneNumber;

	private String accessToken;

	@BeforeEach
	void setUp() {
		// 테스트 시작 전마다 초기화 (유니크 이메일 보장)
		this.email = "test" + System.currentTimeMillis() + "@test.com"; // 항상 다른 이메일
		this.password = "manager0";
		this.firstName = "JAEBIN";
		this.lastName = "CHUNG";
		this.phoneNumber = "01025861111";
	}

	private void changeTestEmailAndPassword() {
		this.email = "test1234@naver.com";
		this.password = "1q2w3e4r!";
	}

	@Test
	@DisplayName("회원가입 전체 시나리오 - 키 발급 및 암호화 포함")
	void signup_flow_with_crypto() throws Exception {

		// 1. RSA 공개키 발급
		MvcResult rsaResult = mockMvc.perform(post("/api/payment/crypto/rsa"))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode rsaResponse = objectMapper.readTree(rsaResult.getResponse().getContentAsString());
		String publicKey = rsaResponse.at("/data/publicKey").asText();

		logger.info("");

		// 2. AES 키 발급
		MvcResult aesResult = mockMvc.perform(post("/api/payment/crypto/aes"))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode aesResponse = objectMapper.readTree(aesResult.getResponse().getContentAsString());
		String aesKey = aesResponse.at("/data/aesKey").asText();

		logger.info("");

		// 3. AES 키를 RSA 공개키로 암호화
		String encAesKey = RsaKeyCryptoUtils.encryptAesKeyWithRsaPublicKey(aesKey, publicKey);

		logger.info("");

		// 4. 평문 비밀번호 AES 키로 암호화
		String encPassword = AesKeyCryptoUtils.encryptPasswordWithAesKey(password, aesKey);

		// 5. SignUpRequest 회원가입 요청 정보 생성
		SignUpRequest request = SignUpRequest.builder()
				.email(email)
				.firstName(firstName)
				.lastName(lastName)
				.phoneNumber(phoneNumber)
				.rsaPublicKey(publicKey)
				.encAesKey(encAesKey)
				.encPassword(encPassword)
				.build();

		// 6. 회원가입 API 호출
		final ResultActions resultActions = mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Test
	@DisplayName("로그인 전체 시나리오 - 키 발급 및 암호화 포함")
	void login_flow_with_crypto() throws Exception {

		// 0. 회원가입
//		this.signup_flow_with_crypto();
		jdbcTemplate.execute("DELETE FROM payment.payment_user_card");
		jdbcTemplate.execute("DELETE FROM payment.payment_user");
		jdbcTemplate.execute("ALTER TABLE payment.payment_user ALTER COLUMN id RESTART WITH 1");
		if(!initSignUpSql.isEmpty()) jdbcTemplate.execute(initSignUpSql);
		else this.signup_flow_with_crypto();

		changeTestEmailAndPassword();

		// 1. RSA 공개키 발급
		MvcResult rsaResult = mockMvc.perform(post("/api/payment/crypto/rsa"))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode rsaResponse = objectMapper.readTree(rsaResult.getResponse().getContentAsString());
		String publicKey = rsaResponse.at("/data/publicKey").asText();

		// 2. AES 키 발급
		MvcResult aesResult = mockMvc.perform(post("/api/payment/crypto/aes"))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode aesResponse = objectMapper.readTree(aesResult.getResponse().getContentAsString());
		String aesKey = aesResponse.at("/data/aesKey").asText();

		// 3. AES 키를 RSA 공개키로 암호화
		String encAesKey = RsaKeyCryptoUtils.encryptAesKeyWithRsaPublicKey(aesKey, publicKey);

		// 4. 평문 비밀번호 AES 키로 암호화
		String encPassword = AesKeyCryptoUtils.encryptPasswordWithAesKey(password, aesKey);

		// 5. 이미 가입된 이메일 사용 (ex. 회원가입 테스트 후 그 데이터로 테스트)

		// 6. 로그인 요청 DTO 생성
		LoginRequest request = LoginRequest.builder()
				.email(email)
				.encPassword(encPassword)
				.encAesKey(encAesKey)
				.rsaPublicKey(publicKey)
				.build();

		// 7. 로그인 API 호출
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		// 8. 응답 헤더에서 토큰 확인
		String jwtHeader = loginResult.getResponse().getHeader("Authorization");
		System.out.println("JWT: " + jwtHeader);

		// 토큰이 실제로 발급되었는지 검증 (단순 체크)
		assertNotNull(jwtHeader);
		assertTrue(jwtHeader.startsWith("Bearer "));
		accessToken = jwtHeader;
		logger.info("accessToken=" + accessToken);
	}

	@Test
	@DisplayName("로그인 후 JWT로 유저 정보 조회 성공")
	void getUserInfo_success() throws Exception {
		// 로그인
		login_flow_with_crypto();

		if (accessToken.isEmpty()) {
			logger.error("accessToken is empty.");
			return;
		}

		// 유저 정보 조회
		final ResultActions resultActions = mockMvc.perform(get("/api/auth/info")
						.header("Authorization", accessToken))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.firstName", notNullValue()))
				.andExpect(jsonPath("$.data.lastName", notNullValue()));

		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Test
	@DisplayName("잘못된 토큰(가짜 토큰) → 401 반환")
	void getUserInfo_invalidToken() throws Exception {

		final ResultActions resultActions = mockMvc.perform(get("/api/auth/info")
						.header("Authorization", "Bearer fake.invalid.token"))
				.andDo(print())
				.andExpect(status().isUnauthorized());

		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Disabled("JWT 만료 테스트는 개발 환경에서만 사용, 빌드에는 포함하지 않음")
	@Test
	@DisplayName("만료된 토큰 → 401 반환")
	void getUserInfo_expiredToken() throws Exception {
		login_flow_with_crypto();

		if (accessToken.isEmpty()) {
			logger.error("accessToken is empty.");
			return;
		}

		//만료를 위한 10초 토큰 세팅 후 sleep(1100)
		Thread.sleep(11000);

		final ResultActions resultActions = mockMvc.perform(get("/api/auth/info")
						.header("Authorization", accessToken))
				.andDo(print())
				.andExpect(status().isUnauthorized());

		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Test
	@DisplayName("토큰 없이 유저 정보 요청 → 401 반환")
	void getUserInfo_noToken() throws Exception {
		login_flow_with_crypto();

		if (accessToken.isEmpty()) {
			logger.error("accessToken is empty.");
			return;
		}
		final ResultActions resultActions = mockMvc.perform(get("/api/auth/info"))
				.andDo(print())
				.andExpect(status().isUnauthorized());

		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}
}