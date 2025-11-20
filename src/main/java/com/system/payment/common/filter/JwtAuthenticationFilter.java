package com.system.payment.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.common.dto.response.Response;
import com.system.payment.user.model.dto.SimpleUserDetails;
import com.system.payment.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final AuthenticationEntryPoint authenticationEntryPoint;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")
				|| authHeader.length() <= 7) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(7);

		try {
			Claims claims = jwtUtils.parseClaims(token);
			Integer userId = claims.get("user_id", Integer.class);

			if (userId == null) {
				SecurityContextHolder.clearContext();
				authenticationEntryPoint.commence(
						request, response,
						new AuthenticationCredentialsNotFoundException("Missing claim: user_id")
				);
				return;
			}

			UserDetails userDetails = new SimpleUserDetails(userId);
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			filterChain.doFilter(request, response);
		} catch (io.jsonwebtoken.ExpiredJwtException ex) {
			logger.error(ex.getMessage() + ": JWT expired");
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(
					request, response, new CredentialsExpiredException("JWT expired", ex)
			);
			return;
		} catch (io.jsonwebtoken.security.SecurityException ex) {
			logger.error(ex.getMessage() + ": Invalid JWT signature");
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(
					request, response,
					new BadCredentialsException("Invalid JWT signature", ex)
			);
			return;
		} catch (MalformedJwtException
				 | UnsupportedJwtException
				 | IllegalArgumentException ex) {
			logger.error(ex.getMessage() + ": Invalid JWT");
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(
					request, response,
					new BadCredentialsException("Invalid JWT", ex)
			);
			return;
		} catch (AuthenticationException ex) {
			logger.error(ex.getMessage() + ": Unauthorized");
			SecurityContextHolder.clearContext();
			authenticationEntryPoint
					.commence(request, response, ex);
			return;
		} catch (AccessDeniedException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.error(ex.getMessage() + ": Internal Server Error");
			SecurityContextHolder.clearContext();

			if (response.isCommitted()) return;

			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("application/json;charset=UTF-8");

			var body = Response.<Void>builder()
					.status(500)
					.message("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
					.data(null)
					.build();

			objectMapper.writeValue(response.getWriter(), body);
			return;
		}
	}
}
