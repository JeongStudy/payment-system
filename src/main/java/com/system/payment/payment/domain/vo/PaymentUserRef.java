package com.system.payment.payment.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Embeddable
public class PaymentUserRef {

    @Column(nullable = false)
    private Integer userId;

    public static PaymentUserRef of(Integer userId) {
        return PaymentUserRef.builder().userId(userId).build();
    }
}