package com.system.payment.payment.controller;

import com.system.payment.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExceptionTestController.class)
@Import({GlobalExceptionHandler.class}) // 예외 핸들러 import
public class ExceptionTestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private static final Logger logger = LoggerFactory.getLogger(ExampleController.class);

	@Test
	void 비즈니스_예외_테스트() throws Exception {
		logger.info("비즈니스_예외_테스트");
		final ResultActions resultActions = mockMvc.perform(get("/api/payment/test/business-error"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(1001))
				.andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다."))
				.andExpect(jsonPath("$.errors").doesNotExist());
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
				.andExpect(jsonPath("$.errors").isArray());
		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Test
	void 널포인터_예외_테스트() throws Exception {
		logger.info("널포인터_예외_테스트");
		final ResultActions resultActions = mockMvc.perform(get("/api/payment/test/null-pointer"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(5000))
				.andExpect(jsonPath("$.message").value("Null Pointer Error"));
		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}

	@Test
	void 일반_예외_테스트() throws Exception {
		logger.info("일반_예외_테스트");
		final ResultActions resultActions = mockMvc.perform(get("/api/payment/test/general-exception"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(5000))
				.andExpect(jsonPath("$.message").value("general-exception"));
		logger.info(resultActions.andReturn().getResponse().getContentAsString());
		logger.info("");
	}
}
