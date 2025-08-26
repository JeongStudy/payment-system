package com.system.payment.filter;

import com.system.payment.config.security.PaymentServerAuthenticationEntryPoint;
import com.system.payment.user.model.dto.SimpleUserDetails;
import com.system.payment.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final PaymentServerAuthenticationEntryPoint paymentServerAuthenticationEntryPoint;

	public JwtAuthenticationFilter(JwtUtils jwtUtils, PaymentServerAuthenticationEntryPoint paymentServerAuthenticationEntryPoint) {
		this.jwtUtils = jwtUtils;
		this.paymentServerAuthenticationEntryPoint = paymentServerAuthenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(7);

		try {
			Claims claims = jwtUtils.parseClaims(token);
			Integer userId = claims.get("user_id", Integer.class);

			UserDetails userDetails = new SimpleUserDetails(userId);
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			filterChain.doFilter(request, response);
		} catch (AuthenticationException ex) {
			logger.error("AuthenticationException : Unauthorized");
			paymentServerAuthenticationEntryPoint
					.commence(request, response, ex);
		} catch (Exception ex) {
			logger.error("Exception : Invalid or expired token");
			paymentServerAuthenticationEntryPoint
					.commence(request, response, new AuthenticationCredentialsNotFoundException("Invalid or expired token", ex));
		}
	}
}
