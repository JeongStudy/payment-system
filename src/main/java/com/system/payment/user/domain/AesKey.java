package com.system.payment.user.domain;

import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerBadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
	private String aesKey;

	@Column(name = "created_timestamp", nullable = false, updatable = false)
	private LocalDateTime createdTimestamp;

	@Column(name = "expired_timestamp", nullable = false)
	private LocalDateTime expiredTimestamp;

	private AesKey(String aesKey, LocalDateTime createdTimestamp, LocalDateTime expiredTimestamp) {
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

		public void validateNotExpired() {
		if (this.expiredTimestamp.isBefore(LocalDateTime.now())) {
			throw new PaymentServerBadRequestException(ErrorCode.INVALID_AES_KEY);
		}
	}
}