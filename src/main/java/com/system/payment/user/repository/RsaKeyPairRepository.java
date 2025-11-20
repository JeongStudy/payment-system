package com.system.payment.user.repository;

import com.system.payment.common.exception.ErrorCode;
import com.system.payment.common.exception.PaymentServerNotFoundException;
import com.system.payment.user.domain.RsaKeyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RsaKeyPairRepository extends JpaRepository<RsaKeyPair, Integer> {
    Optional<RsaKeyPair> findByPublicKey(String publicKey);

    default RsaKeyPair getByPublicKeyOrThrow(String publicKey) {
		return findByPublicKey(publicKey)
				.orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.RSA_KEY_NOT_FOUND));
	}
}