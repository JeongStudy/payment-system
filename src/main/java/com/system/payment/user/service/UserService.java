package com.system.payment.user.service;

import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerNotFoundException;
import com.system.payment.exception.PaymentServerUnauthorizedException;
import com.system.payment.provider.AuthUserProvider;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.model.reponse.UserResponse;
import com.system.payment.user.repository.PaymentUserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	private final AuthUserProvider authUserProvider;
	private final PaymentUserRepository paymentUserRepository;

	public UserService(AuthUserProvider authUserProvider, PaymentUserRepository paymentUserRepository) {
		this.authUserProvider = authUserProvider;
		this.paymentUserRepository = paymentUserRepository;
	}

	public PaymentUser findUser() {
		Integer userId = authUserProvider.getUserId();
		if(userId == null) throw new PaymentServerUnauthorizedException(ErrorCode.UNAUTHORIZED);
		return paymentUserRepository.getByIdOrThrow(userId);
	}

	public UserResponse getUser() {
		final PaymentUser paymentUser = findUser();
		return UserResponse.from(paymentUser);
	}
}