package com.system.payment.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodRef {
  @Enumerated(EnumType.STRING)
  @Column(length=30, nullable=false)
  private PaymentMethodType paymentMethodType;

  @Column(nullable=false)
  private Integer paymentMethodId;
}