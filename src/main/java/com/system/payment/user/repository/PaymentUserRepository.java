package com.system.payment.user.repository;

import com.system.payment.user.domain.jaebin.PaymentUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentUserRepository extends JpaRepository<PaymentUser, Long> {
	Optional<PaymentUser> findByEmail(String email);

	boolean existsByEmail(String email);
}