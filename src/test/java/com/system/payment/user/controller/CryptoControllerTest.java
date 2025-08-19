package com.system.payment.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.payment.service.PaymentProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
//@ActiveProfiles("integration")
class CryptoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PaymentProducer paymentProducer;  // 메시지 발행 막기

	@MockitoBean
	private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry; // Listener 자체 Mock

	private static final Logger logger = LoggerFactory.getLogger(CryptoControllerTest.class);

	@Test
	@DisplayName("AES 키 발급 성공 테스트")
	void generateAesKey_success() throws Exception {

		final ResultActions resultActions = mockMvc.perform(post("/api/payment/crypto/aes")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(2003))
				.andExpect(jsonPath("$.message").value("AES Key가 생성되었습니다."));
		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Test
	@DisplayName("RSA 키 발급 성공 테스트")
	void generateRsaKey_success() throws Exception {
		final ResultActions resultActions = mockMvc.perform(post("/api/payment/crypto/rsa")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(2002))
				.andExpect(jsonPath("$.message").value("RSA Key가 생성되었습니다."));

		logger.info(resultActions.andReturn().getResponse().getContentAsString());
	}
}
