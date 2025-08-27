package com.system.payment.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.payment.domain.Payment;
import com.system.payment.payment.domain.outbox.EventType;
import com.system.payment.payment.domain.outbox.OutboxEvent;
import com.system.payment.payment.repository.OutboxEventRepository;
import com.system.payment.payment.scheduler.OutboxPublishWorker;
import com.system.payment.payment.service.PaymentProducer;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.model.request.LoginRequest;
import com.system.payment.util.AesKeyCryptoUtils;
import com.system.payment.util.RsaKeyCryptoUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // 👈 추가
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentRequestControllerTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	OutboxEventRepository outboxEventRepository;

	@Autowired
	OutboxPublishWorker outboxPublishWorker;

	@MockitoBean
	private PaymentProducer paymentProducer;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Value("${sql.init-sign-up-secret-sql}")
	String initSignUpSql;

	@Value("${sql.init-card-register-secret-sql}")
	String initCardRegisterSql;

	private static final Logger logger = LoggerFactory.getLogger(PaymentRequestControllerTest.class);


	private String email;
	private String accessToken;
	private String password;

	@BeforeAll
	void setUp() {
		jdbcTemplate.execute(initSignUpSql);
		jdbcTemplate.execute(initCardRegisterSql);
		this.email = "test1234@naver.com";
		this.password = "1q2w3e4r!";
	}

	private void login_flow_with_crypto() throws Exception {

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
	}

	// 편의: 멱등키 발급 API 호출
	private String issueIdempotencyKey() throws Exception {
		var res = mockMvc.perform(get("/api/payments/requests/idempotency-key")
						.header("Authorization", accessToken))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andReturn();
		Map<?, ?> body = objectMapper.readValue(res.getResponse().getContentAsByteArray(), Map.class);
		Map<?, ?> data = (Map<?, ?>) body.get("data");
		return (String) data.get("idempotencyKey");
	}

	// 편의: 결제요청 API 바디 생성 (필드는 프로젝트 DTO에 맞춰 수정)
	private String paymentCreateJson(String idemKey) throws Exception {
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

		logger.info("idempotencyKey=" + idemKey);

		var req = Map.of(
				"idempotencyKey", idemKey,
				"paymentUserCardId", 1,
				"productName", "AI 라이센스 키(연 1석)",
				"rsaPublicKey", publicKey,
				"encAesKey", encAesKey,
				"encPassword", encPassword,
				"amount", 1,
				"serviceOrderId", "2400811"
		);
		return objectMapper.writeValueAsString(req);
	}

	@Test
	void flow_idempotency_then_create_saves_outbox() throws Exception {
		this.login_flow_with_crypto();

		// 1) 멱등키 발급
		String idem = issueIdempotencyKey();

		// 2) 결제요청
		final MvcResult mvcResult = mockMvc.perform(post("/api/payments/requests")
						.header("Authorization", accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(paymentCreateJson(idem)))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		JsonNode requsetRes = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
		Integer eventId = requsetRes.at("/data/eventId").asInt();

		// 3) Outbox 저장 확인
		Optional<OutboxEvent> event = outboxEventRepository.findById(eventId);
		OutboxEvent e = event.get();
		assertThat(e.getEventType()).isEqualTo(EventType.PAYMENT_REQUESTED_V1); // :contentReference[oaicite:2]{index=2}
		assertThat(e.getStatus()).isIn("PENDING");
		assertThat(e.getPayload()).isNotBlank();
		assertThat(e.getEventKey()).isNotBlank(); // txId
	}

	@Test
	void outbox_scheduler_publishes_to_kafka() throws Exception {
		// 0) 로그인 플로우
		this.login_flow_with_crypto();

		// 1) 멱등키 발급
		String idem = issueIdempotencyKey();

		// 2) 결제요청 -> Outbox 적재
		final MvcResult mvcResult = mockMvc.perform(post("/api/payments/requests")
						.header("Authorization", accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(paymentCreateJson(idem)))
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode requestRes = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
		Integer eventId = requestRes.at("/data/eventId").asInt();
		assertThat(eventId).isNotNull();

		// 3) Worker 강제 실행
		outboxPublishWorker.processOne(eventId);

		// 4) PaymentProducer 호출 검증
		ArgumentCaptor<Payment> paymentCap = ArgumentCaptor.forClass(Payment.class);
		ArgumentCaptor<PaymentUser> userCap = ArgumentCaptor.forClass(PaymentUser.class);
		ArgumentCaptor<PaymentUserCard> cardCap = ArgumentCaptor.forClass(PaymentUserCard.class);
		ArgumentCaptor<String> productCap = ArgumentCaptor.forClass(String.class);

		verify(paymentProducer, times(1)).sendPaymentRequested(
				paymentCap.capture(),
				userCap.capture(),
				cardCap.capture(),
				productCap.capture()
		);

		assertThat(paymentCap.getValue()).isNotNull();
		assertThat(userCap.getValue()).isNotNull();
		assertThat(cardCap.getValue()).isNotNull();
		assertThat(productCap.getValue()).isEqualTo("AI 라이센스 키(연 1석)");

		logger.info("✅ Outbox event {} successfully published via PaymentProducer mock.", eventId);
	}
}
