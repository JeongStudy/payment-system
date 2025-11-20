package com.system.payment.user.service;

import com.system.payment.common.exception.ErrorCode;
import com.system.payment.common.exception.PaymentServerUnauthorizedException;
import com.system.payment.provider.AuthUserProvider;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.model.reponse.UserResponse;
import com.system.payment.user.repository.PaymentUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
	private final AuthUserProvider authUserProvider;
	private final PaymentUserRepository paymentUserRepository;

	@Transactional
	public PaymentUser findUser() {
		Integer userId = authUserProvider.getUserId();
		if(userId == null) throw new PaymentServerUnauthorizedException(ErrorCode.UNAUTHORIZED);
		return paymentUserRepository.getByIdOrThrow(userId);
	}

	@Transactional
	public UserResponse getUser() {
		final PaymentUser paymentUser = findUser();
		return UserResponse.from(paymentUser);
	}
}