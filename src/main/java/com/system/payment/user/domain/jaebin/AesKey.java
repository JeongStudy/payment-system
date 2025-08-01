package com.system.payment.user.domain.jaebin;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "aes_key", schema = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class AesKey {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "aes_key", nullable = false, unique = true)
	private String aesKey; // UUID 문자열

	@Column(name = "created_timestamp", nullable = false, updatable = false)
	private LocalDateTime createdTimestamp;

	@Column(name = "expired_timestamp", nullable = false)
	private LocalDateTime expiredTimestamp;

	public AesKey(String aesKey, LocalDateTime createdTimestamp, LocalDateTime expiredTimestamp) { // 도메인 생성자
		this.aesKey = aesKey;
		this.createdTimestamp = createdTimestamp;
		this.expiredTimestamp = expiredTimestamp;
	}

	public static AesKey create(String aesKey) {
		LocalDateTime now = LocalDateTime.now();
		Duration ttl = Duration.ofMinutes(3);
		return new AesKey(
				aesKey,
				now,
				now.plus(ttl)
		);
	}
}