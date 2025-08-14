package com.system.payment.payment.repository;

import com.system.payment.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
	boolean existsByIdempotencyKey(String IdempotencyKey);
	Optional<Payment> findByIdempotencyKey(String IdempotencyKey);
}
