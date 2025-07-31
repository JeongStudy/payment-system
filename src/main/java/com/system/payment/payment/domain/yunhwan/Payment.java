package com.system.payment.payment.domain.yunhwan;

import com.system.payment.common.domain.yunhwan.BaseEntity;
import com.system.payment.user.domain.yunhwan.PaymentUser;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "payment", schema = "payment")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private PaymentUser user;

    @Column
    private Integer referenceId;

    @Column(length = 20)
    private String referenceType;

    @Column(length = 30, nullable = false)
    private String paymentMethodType;

    @Column(nullable = false)
    private Integer paymentMethodId;

    @Column(length = 30, nullable = false)
    private String paymentType;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(length = 2, nullable = false)
    private String paymentResultCode;

    @Column(nullable = false)
    private LocalDateTime requestedTimestamp;

    @Column
    private LocalDateTime approvedTimestamp;

    @Column
    private LocalDateTime canceledTimestamp;

    @Column
    private LocalDateTime failedTimestamp;

    @Column(length = 200)
    private String externalPaymentId;

    @Column(length = 20)
    private String errorCode;

    @Column(length = 300)
    private String errorMessage;

    @Column(length = 100, nullable = false)
    private String idempotencyKey;

    @Column(length = 100, nullable = false)
    private String transactionId;
}
