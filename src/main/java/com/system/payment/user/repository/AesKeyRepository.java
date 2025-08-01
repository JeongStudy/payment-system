package com.system.payment.user.repository;

import com.system.payment.user.domain.jaebin.AesKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AesKeyRepository extends JpaRepository<AesKey, Integer> {
    boolean existsByAesKey(String aesKey);
	Optional<AesKey> findByIdAndExpiredTimestampAfter(Integer id, LocalDateTime now);
}