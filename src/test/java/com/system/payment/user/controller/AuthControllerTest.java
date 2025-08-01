package com.system.payment.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.user.service.AuthService;
import com.system.payment.util.AesKeyCryptoUtil;
import com.system.payment.util.RsaKeyCryptoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static final Logger logger = LoggerFactory.getLogger(AuthControllerTest.class);

	@Test
	@DisplayName("회원가입 전체 시나리오 - 키 발급 및 암호화 포함")
	void signup_flow_with_crypto() throws Exception {

		MvcResult rsaResult = mockMvc.perform(post("/api/payment/crypto/rsa"))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode rsaResponse = objectMapper.readTree(rsaResult.getResponse().getContentAsString());
		String publicKey = rsaResponse.at("/data/publicKey").asText();

		logger.info("");

		MvcResult aesResult = mockMvc.perform(post("/api/payment/crypto/aes"))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode aesResponse = objectMapper.readTree(aesResult.getResponse().getContentAsString());
		String aesKey = aesResponse.at("/data/aesKey").asText();

		logger.info("");

		String encAesKey = RsaKeyCryptoUtil.encryptAesKeyWithRsaPublicKey(aesKey, publicKey);

		logger.info("");

		String password = "manager0";
		String encPassword = AesKeyCryptoUtil.encryptPasswordWithAesKey(password, aesKey);

		String email = "jaebin1291@naver.com";
		String firstName = "JAEBIN";
		String lastName = "CHUNG";
		String phoneNumber = "01025861111";

		SignUpRequest request = SignUpRequest.builder()
				.email(email)
				.firstName(firstName)
				.lastName(lastName)
				.phoneNumber(phoneNumber)
				.publicKey(publicKey)
				.encAesKey(encAesKey)
				.encPassword(encPassword)
				.build();

		final ResultActions resultActions = mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}
}