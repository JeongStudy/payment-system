package com.system.payment.payment.domain.entity;

import com.system.payment.common.domain.entity.BaseEntity;
import com.system.payment.payment.domain.constant.PaymentState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "payment_idempotency", schema = "payment",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_idempotency_key", columnNames = "idempotency_key"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PaymentIdempotency extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentState state;

    // 처리 완료로 전이
    public void markDone() {
        if (this.state == PaymentState.DONE) return;
        this.state = PaymentState.DONE;
    }
}
