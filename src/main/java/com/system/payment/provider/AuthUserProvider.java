package com.system.payment.provider;

import com.system.payment.common.exception.ErrorCode;
import com.system.payment.common.exception.PaymentServerUnauthorizedException;
import com.system.payment.user.model.dto.SimpleUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUserProvider {
	public SimpleUserDetails getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null ||
				!authentication.isAuthenticated() ||
				authentication.getPrincipal() == null ||
				authentication.getPrincipal().equals("anonymousUser")) {
			throw new PaymentServerUnauthorizedException(ErrorCode.UNAUTHORIZED);
		}

		Object principal = authentication.getPrincipal();
		if (!(principal instanceof SimpleUserDetails)) {
			throw new PaymentServerUnauthorizedException(ErrorCode.UNAUTHORIZED);
		}
		return (SimpleUserDetails) principal;
	}

	public Integer getUserId() {
		return getUser().getUserId();
	}
}
