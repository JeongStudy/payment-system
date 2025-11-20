package com.system.payment.user.repository;

import com.system.payment.common.exception.ErrorCode;
import com.system.payment.common.exception.PaymentServerNotFoundException;
import com.system.payment.user.domain.PaymentUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentUserRepository extends JpaRepository<PaymentUser, Integer> {
	Optional<PaymentUser> findByEmail(String email);

	boolean existsByEmail(String email);

	default PaymentUser getByIdOrThrow(Integer userId) {
		return findById(userId)
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.USER_NOT_EXIST));
	}

	default PaymentUser getByEmailOrThrow(String email) {
		return findByEmail(email)
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.USER_NOT_EXIST));
	}
}