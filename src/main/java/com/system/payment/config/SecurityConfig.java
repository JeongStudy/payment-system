package com.system.payment.config;

import com.system.payment.common.filter.JwtAuthenticationFilter;
import com.system.payment.common.filter.AuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter, AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors
						.configurationSource(request -> {
							CorsConfiguration config = new CorsConfiguration();
							config.setAllowedOriginPatterns(List.of("*"));
							config.setAllowedMethods(List.of("*"));
							config.setAllowedHeaders(List.of("*"));
							config.setAllowCredentials(true);
							config.setExposedHeaders(List.of("Authorization"));
							return config;
						})
				)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/payment/crypto/aes",
								"/api/payment/crypto/rsa",
								"/api/payment/crypto/encrypt/password",
								"/api/payment/crypto/encrypt/aes",
								"/api/auth/login",
								"/api/auth/signup",
								"/api/payment/health-check",
								"/api/payment/test/*",
								"/api/payment/cards/inicis/return").permitAll()
						.anyRequest().authenticated()
				)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(exceptionHandling ->
						exceptionHandling.authenticationEntryPoint(authenticationEntryPoint)
				);
		;

		return http.build();
	}
}
