package com.system.payment.user.repository;

import com.system.payment.user.domain.jaebin.AesKey;
import com.system.payment.user.domain.jaebin.RsaKeyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RsaKeyPairRepository extends JpaRepository<RsaKeyPair, Integer> {
    Optional<RsaKeyPair> findByIdAndExpiredTimestampAfter(Integer id, LocalDateTime now);
}