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
@Table(name = "rsa_key_pair", schema = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RsaKeyPair {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "public_key", nullable = false, unique = true, columnDefinition = "text")
	private String publicKey;

	@Column(name = "private_key", nullable = false, unique = true, columnDefinition = "text")
	private String privateKey;

	@Column(name = "expired_timestamp", nullable = false)
	private LocalDateTime expiredTimestamp;

	@Column(name = "created_timestamp", nullable = false)
	private LocalDateTime createdTimestamp = LocalDateTime.now();

	private RsaKeyPair(String publicKey, String privateKey, LocalDateTime createdTimestamp, LocalDateTime expiredTimestamp) {
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.createdTimestamp = createdTimestamp;
		this.expiredTimestamp = expiredTimestamp;
	}

	public static RsaKeyPair create(String publicKey, String privateKey) {
		LocalDateTime now = LocalDateTime.now();
		Duration ttl = Duration.ofMinutes(3);

		return new RsaKeyPair(
				publicKey,
				privateKey,
				now,
				now.plus(ttl)
		);
	}

	public void validateNotExpired() {
		if (this.expiredTimestamp.isBefore(LocalDateTime.now())) {
			throw new PaymentServerBadRequestException(ErrorCode.INVALID_RSA_KEY);
		}
	}
}