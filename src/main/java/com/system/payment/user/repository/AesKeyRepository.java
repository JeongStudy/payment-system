package com.system.payment.user.repository;

import com.system.payment.user.domain.AesKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AesKeyRepository extends JpaRepository<AesKey, Integer> {
    boolean existsByAesKey(String aesKey);
	Optional<AesKey> findByAesKey(String aesKey);
}