package com.system.payment.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtils {
	@Value("${jwt.secret}")
	private String SECRET;

	public String generateToken(Integer userId) {
		return Jwts.builder()
				.claim("user_id", userId)
				.setIssuedAt(new Date())
				.setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
//				.setExpiration(Date.from(Instant.now().plus(10, ChronoUnit.SECONDS)))
				.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
				.compact();
	}

	public Claims parseClaims(String token){
		return Jwts.parserBuilder()
				.setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

}