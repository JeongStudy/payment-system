package com.system.payment.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.common.dto.response.ErrorCode;
import com.system.payment.common.dto.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {
	private final ObjectMapper objectMapper;

	public AuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
						 AuthenticationException authException) throws IOException {
		Response<Void> errorResponse = Response.<Void>builder()
				.status(ErrorCode.UNAUTHORIZED.getStatus())
				.message(ErrorCode.UNAUTHORIZED.getMessage())
				.data(null)
				.build();
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}
}
