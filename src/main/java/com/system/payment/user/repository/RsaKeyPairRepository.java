package com.system.payment.user.repository;

import com.system.payment.user.domain.RsaKeyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RsaKeyPairRepository extends JpaRepository<RsaKeyPair, Integer> {
    Optional<RsaKeyPair> findByPublicKey(String publicKey);
}