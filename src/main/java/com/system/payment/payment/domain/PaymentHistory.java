package com.system.payment.payment.domain;

import com.system.payment.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history", schema = "payment")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PaymentHistory extends BaseEntity{

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

    // SYSTEM, PG_API, KAFKA_CONSUMER, ADMIN, SCHEDULER ...
    @Column(nullable = false, length = 30)
    private String changedBy;

    @Column(length = 200)
    private String changedReason;

    // jsonb - 의존성 없이 String으로 매핑
    @Column(columnDefinition = "jsonb")
    private String prevData;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String newData;

    @Column(columnDefinition = "jsonb")
    private String externalResponse;

    @Column(nullable = false, length = 100)
    private String transactionId;
}
