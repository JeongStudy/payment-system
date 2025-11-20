package com.system.payment.card.repository;

import com.system.payment.card.domain.constant.BillingKeyStatus;
import com.system.payment.card.domain.entity.PaymentUserCard;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PaymentUserCardRepository extends JpaRepository<PaymentUserCard, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) // FOR UPDATE
    Optional<PaymentUserCard> findByPgOid(String oid);

    List<PaymentUserCard> findByUser_IdAndIsDeletedFalseAndBillingKeyStatus(
            Integer userId,
            BillingKeyStatus status,
            Sort sort
    );
}
