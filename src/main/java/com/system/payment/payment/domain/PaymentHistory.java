package com.system.payment.payment.domain;

import com.system.payment.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history", schema = "payment")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PaymentHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	@Column(length = 2)
	private String prevResultCode;

	@Column(nullable = false, length = 2)
	private String newResultCode;

	@Column(nullable = false)
	private LocalDateTime changedAt;

	// SYSTEM, PG_API, KAFKA_CONSUMER, ADMIN, SCHEDULER
	@Column(nullable = false, length = 30)
	private String changedBy;

	@Column(length = 200)
	private String changedReason;

	@Column(columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String prevData;

	@Column(columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private String newData;

	@Column(columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String externalResponse;

	@Column(nullable = false, length = 100)
	private String transactionId;

	private PaymentHistory(Payment payment, String newResultCode, LocalDateTime changedAt,
						   String changedBy, String changedReason, String newData,
						   String transactionId) {
		this.payment = payment;
		this.newResultCode = newResultCode;
		this.changedAt = changedAt;
		this.changedBy = changedBy;
		this.changedReason = changedReason;
		this.newData = newData;
		this.transactionId = transactionId;
	}

	public static PaymentHistory create(Payment payment, String newResultCode, LocalDateTime changedAt,
										String changedBy, String changedReason, String newData,
										String transactionId) {
		return new PaymentHistory(
				payment,
				newResultCode,
				changedAt,
				changedBy,
				changedReason,
				newData,
				transactionId
		);
	}
}
