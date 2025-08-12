package com.system.payment.payment.repository;

import com.system.payment.payment.domain.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Integer> {
}
