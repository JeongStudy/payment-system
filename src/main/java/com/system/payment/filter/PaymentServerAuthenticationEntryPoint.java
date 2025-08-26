package com.system.payment.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.exception.ErrorCode;
import com.system.payment.util.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PaymentServerAuthenticationEntryPoint implements AuthenticationEntryPoint {
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
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }
}
