package com.system.payment.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.payment.service.PaymentProducer;
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
class ExceptionTestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PaymentProducer paymentProducer;  // 메시지 발행 막기

	@MockitoBean
	private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry; // Listener 자체 Mock

	private static final Logger logger = LoggerFactory.getLogger(ExceptionTestControllerTest.class);

	@Test
	void 비즈니스_예외_테스트() throws Exception {
		logger.info("비즈니스_예외_테스트");
		final ResultActions resultActions = mockMvc.perform(get("/api/payment/test/business-error"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(4000))
				.andExpect(jsonPath("$.message").value("Bad Request Parameter"))
				.andExpect(jsonPath("$.errors").doesNotExist())
				.andDo(print());

		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Test
	void 파라미터_검증_실패_테스트() throws Exception {
		logger.info("파라미터_검증_실패_테스트");
		String body = "{\"name\": \"\"}";
		final ResultActions resultActions = mockMvc.perform(post("/api/payment/test/validation-error")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(4000))
				.andExpect(jsonPath("$.message").value("Bad Request Parameter"))
				.andExpect(jsonPath("$.errors").isArray())
				.andDo(print());
		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Test
	void 널포인터_예외_테스트() throws Exception {
		logger.info("널포인터_예외_테스트");
		final ResultActions resultActions = mockMvc.perform(get("/api/payment/test/null-pointer"))
				.andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(5001))
				.andExpect(jsonPath("$.message").value("Server Null Pointer Error"));
		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Test
	void 일반_예외_테스트() throws Exception {
		logger.info("일반_예외_테스트");
		final ResultActions resultActions = mockMvc.perform(get("/api/payment/test/general-exception"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(5000))
				.andDo(print());
		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}
}
