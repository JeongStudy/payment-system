package com.system.payment.payment.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.outbox.domain.constant.EventType;
import com.system.payment.outbox.domain.entity.OutboxEvent;
import com.system.payment.outbox.repository.OutboxEventRepository;
import com.system.payment.outbox.worker.OutboxPublishWorker;
import com.system.payment.payment.consumner.PaymentConsumer;
import com.system.payment.payment.producer.PaymentProducer;
import com.system.payment.user.model.request.LoginRequest;
import com.system.payment.common.util.AesKeyCryptoUtils;
import com.system.payment.common.util.RsaKeyCryptoUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 2, topics = {"payment.requested.v1"})
public class PaymentProducerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	OutboxEventRepository outboxEventRepository;

	@Autowired
	TestSink testSink;

	@Autowired
	OutboxPublishWorker outboxPublishWorker;

	@Autowired
	KafkaListenerEndpointRegistry registry;

	@Autowired
	EmbeddedKafkaBroker embeddedKafka;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	public PaymentConsumer paymentConsumer;

	@Autowired
	public PaymentProducer paymentProducer;

	@Value("${sql.init-sign-up-secret-sql}")
	public String initSignUpSql;

	@Value("${sql.init-card-register-secret-sql}")
	public String initCardRegisterSql;

	@Value("${payment.request.topic}")
	private String PAYMENT_REQUESTED_TOPIC;

	private static final Logger logger = LoggerFactory.getLogger(PaymentProducerTest.class);

	@DynamicPropertySource
	static void kafkaProps(DynamicPropertyRegistry r) {
		// 공통
		r.add("spring.kafka.bootstrap-servers",
				() -> System.getProperty("spring.embedded.kafka.brokers")); // ✅ 이 방식 사용

		// Producer
		r.add("spring.kafka.producer.key-serializer",
				() -> "org.apache.kafka.common.serialization.StringSerializer");
		r.add("spring.kafka.producer.value-serializer",
				() -> "org.springframework.kafka.support.serializer.JsonSerializer");

		// yml의 producer.properties.* 매핑
		r.add("spring.kafka.producer.properties.partitioner.class",
				() -> "com.system.payment.payment.partitioner.ConsistentHashPartitioner");
		r.add("spring.kafka.producer.properties.enable.idempotence", () -> "true");
		r.add("spring.kafka.producer.properties.acks", () -> "all");
		r.add("spring.kafka.producer.properties.retries", () -> "3");
		r.add("spring.kafka.producer.properties.max.in.flight.requests.per.connection", () -> "5");

		// Consumer
		r.add("spring.kafka.consumer.group-id", () -> "payment-consumer"); // ← 요구사항
		r.add("spring.kafka.consumer.auto-offset-reset", () -> "latest");
		r.add("spring.kafka.consumer.key-deserializer",
				() -> "org.apache.kafka.common.serialization.StringDeserializer");
		r.add("spring.kafka.consumer.value-deserializer",
				() -> "org.springframework.kafka.support.serializer.JsonDeserializer");
		r.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");

		// yml의 consumer 타임아웃/폴링 관련은 Boot에서 properties.* 로 넘기는게 안전
		r.add("spring.kafka.consumer.properties.heartbeat.interval.ms", () -> "3000");
		r.add("spring.kafka.consumer.properties.session.timeout.ms", () -> "30000");
		r.add("spring.kafka.consumer.properties.max.poll.interval.ms", () -> "300000");
	}

	private String email;
	private String accessToken;
	private String password;

	void waitForTestSinkReady() {
		// test-sink 는 @KafkaListener(id="test-sink") 와 반드시 동일해야 합니다.
		MessageListenerContainer container = registry.getListenerContainer("test-sink");
		assertThat(container).as("test-sink listener must exist").isNotNull();

		// 파티션 할당(assignment) 완료까지 대기 — 여기서 latest 기준점이 “지금”으로 고정됩니다.
		ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());

		// 혹시 이전 테스트의 카운트다운이 남아있지 않도록 초기화
		testSink.reset();
	}

	@BeforeAll
	void setUp() {
		jdbcTemplate.execute("DELETE FROM payment.payment_user_card");
		jdbcTemplate.execute("DELETE FROM payment.payment_user");
		jdbcTemplate.execute("ALTER TABLE payment.payment_user_card ALTER COLUMN id RESTART WITH 1");
		jdbcTemplate.execute("ALTER TABLE payment.payment_user ALTER COLUMN id RESTART WITH 1");
		jdbcTemplate.execute(initSignUpSql);
		jdbcTemplate.execute(initCardRegisterSql);
		this.email = "test1234@naver.com";
		this.password = "1q2w3e4r!";
		waitForTestSinkReady();
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
	void end_to_end_with_embedded_kafka() throws Exception {
		this.login_flow_with_crypto();

		// 1) 멱등키 → 2) 결제요청
		String idem = issueIdempotencyKey();

		final MvcResult mvcResult = mockMvc.perform(post("/api/payments/requests")
						.header("Authorization", accessToken)
						.contentType("application/json")
						.content(paymentCreateJson(idem)))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		JsonNode requsetRes = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
		Integer eventId = requsetRes.at("/data/eventId").asInt();
		Integer paymentId = requsetRes.at("/data/paymentId").asInt();

		// 3) Outbox 저장 확인
		Optional<OutboxEvent> event = outboxEventRepository.findById(eventId);
		OutboxEvent e = event.get();
		assertThat(e.getEventType()).isEqualTo(EventType.PAYMENT_REQUESTED_V1); // :contentReference[oaicite:2]{index=2}
		assertThat(e.getStatus()).isIn("PENDING");
		assertThat(e.getPayload()).isNotBlank();
		assertThat(e.getEventKey()).isNotBlank(); // txId

		//상태 폴링 1회
		var poll = mockMvc.perform(get("/api/payments/requests/status/" + paymentId)
						.header("Authorization", accessToken))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();
		Map<?, ?> statusBody = objectMapper.readValue(poll.getResponse().getContentAsByteArray(), Map.class);
		Map<?, ?> data = (Map<?, ?>) statusBody.get("data");
		assertThat(data.get("code")).isIn("00", "11", "22", "33", "44");

		// 4) (실제) 스케줄러 1회 트리거 (프로젝트 메서드로 교체)
		outboxPublishWorker.processOne(eventId);

		// 5) 테스트 싱크가 메시지 수신했는지 확인
		boolean received = testSink.await(15, TimeUnit.SECONDS);
		assertThat(received).isTrue();
		assertThat(testSink.lastRecord).isNotNull();
		assertThat(testSink.lastRecord.topic()).isEqualTo(PAYMENT_REQUESTED_TOPIC); // :contentReference[oaicite:4]{index=4}

		//상태 폴링 1회
		poll = mockMvc.perform(get("/api/payments/requests/status/" + paymentId)
						.header("Authorization", accessToken))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();
		statusBody = objectMapper.readValue(poll.getResponse().getContentAsByteArray(), Map.class);
		data = (Map<?, ?>) statusBody.get("data");
		assertThat(data.get("code")).isIn("00", "11", "22", "33", "44");

		// 3) Outbox 저장 확인
		event = outboxEventRepository.findById(eventId);
		e = event.get();
		assertThat(e.getEventType()).isEqualTo(EventType.PAYMENT_REQUESTED_V1); // :contentReference[oaicite:2]{index=2}
		assertThat(e.getStatus()).isIn("SENT", "DEAD");
		assertThat(e.getPayload()).isNotBlank();
		assertThat(e.getEventKey()).isNotBlank(); // txId

		logger.info("");

	}

	// 테스트 싱크
	@TestConfiguration
	static class TestSink {
		private CountDownLatch latch = new CountDownLatch(1);
		volatile ConsumerRecord<String, Object> lastRecord;

		void reset() {
			lastRecord = null;
			latch = new CountDownLatch(1);
		}

		boolean await(long t, TimeUnit u) throws InterruptedException {
			return latch.await(t, u);
		}

		@KafkaListener(
				id = "test-sink",
				topics = "payment.requested.v1",
				groupId = "payment-consumer-test",
				properties = {"auto.offset.reset=latest",
						"spring.json.trusted.packages=*"}
		)
		void onMsg(ConsumerRecord<String, Object> rec) {
			lastRecord = rec;
			latch.countDown();
		}
	}
}
