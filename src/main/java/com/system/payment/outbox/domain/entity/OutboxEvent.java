package com.system.payment.outbox.domain.entity;

import com.system.payment.common.domain.entity.BaseEntity;
import com.system.payment.outbox.domain.constant.EventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event", schema = "payment",
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
	@Enumerated(EnumType.STRING)
	private EventType eventType;

	@Column(nullable = false)
	private String eventKey; // e.g. transactionId (카프카 key 겸 멱등키)

	@Column(nullable = false, columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String payload;

	@Column(nullable = false)
	private String status; // PENDING/SENT/FAILED

	@Column(nullable = false)
	private Integer attempts; // 재시도 횟수

	@Column(nullable = false)
	private LocalDateTime nextAttemptAt; // 재시도 타이밍을 제어하는 스케줄 타임스탬프

	@Column(columnDefinition = "text")
	private String lastError;


	@PrePersist
	void prePersist() {
		this.status = this.status == null ? "PENDING" : this.status;
		this.attempts = this.attempts == null ? 0 : this.attempts;
		LocalDateTime now = LocalDateTime.now();
		this.nextAttemptAt = now;
	}

}
