package com.system.payment.card.repository;

import com.system.payment.card.domain.BillingKeyStatus;
import com.system.payment.card.domain.PaymentUserCard;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentUserCardRepository extends JpaRepository<PaymentUserCard, Integer> {
    Optional<PaymentUserCard> findByOid(String oid);

    List<PaymentUserCard> findByUser_IdAndIsDeletedFalseAndBillingKeyStatus(
            Integer userId,
            BillingKeyStatus status,
            Sort sort
    );
}
