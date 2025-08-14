package com.system.payment.payment.domain.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event",
		indexes = @Index(name = "idx_outbox_status_next", columnList = "status,nextAttemptAt"),
		uniqueConstraints = @UniqueConstraint(name = "uk_outbox_key_type", columnNames = {"eventKey", "eventType"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false)
	private String eventType;     // e.g. "PAYMENT_REQUESTED_V1"
	@Column(nullable = false)
	private String eventKey;      // e.g. transactionId (카프카 key 겸 멱등키)
	@Column(nullable = false, columnDefinition = "jsonb")   // RDB별 타입 조정
	private String payload;                               // JSON(아래 Args)
	@Column(nullable = false)
	private String status;        // PENDING/SENT/FAILED
	@Column(nullable = false)
	private Integer attempts;     // 재시도 횟수
	@Column(nullable = false)
	private LocalDateTime nextAttemptAt;
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		this.status = this.status == null ? "PENDING" : this.status;
		this.attempts = this.attempts == null ? 0 : this.attempts;
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
		this.nextAttemptAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
