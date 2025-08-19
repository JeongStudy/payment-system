package com.system.payment.payment.domain.outbox;

import com.system.payment.common.domain.BaseEntity;
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
@Builder(toBuilder = true)
public class OutboxEvent extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
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
	private LocalDateTime nextAttemptAt; // 재시도 타이밍을 제어하는 스케줄 타임스탬프
	@Column(columnDefinition = "text")
	private String lastError; // 재시도 타이밍을 제어하는 스케줄 타임스탬프


	@PrePersist
	void prePersist() {
		this.status = this.status == null ? "PENDING" : this.status;
		this.attempts = this.attempts == null ? 0 : this.attempts;
		LocalDateTime now = LocalDateTime.now();
		this.nextAttemptAt = now;
	}

}
