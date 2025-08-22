package com.system.payment.payment.repository;

import com.system.payment.payment.domain.PaymentIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PaymentIdempotencyRepository extends JpaRepository<PaymentIdempotency, Long> {
    Optional<PaymentIdempotency> findByIdempotencyKey(String key);
}