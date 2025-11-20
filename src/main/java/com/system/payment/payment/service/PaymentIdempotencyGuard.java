package com.system.payment.payment.service;

import com.system.payment.payment.domain.entity.PaymentIdempotency;
import com.system.payment.payment.domain.constant.PaymentState;
import com.system.payment.payment.repository.PaymentIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentIdempotencyGuard {

    private final PaymentIdempotencyRepository idempotencyRepository;

    /**
     * idempotencyKey로 처리 권한을 획득 시 true.
     * 이미 처리(혹은 처리 중)라면 false.
     */
    @Transactional
    public boolean tryAcquire(String idempotencyKey) {
        try {
            PaymentIdempotency row = PaymentIdempotency.builder()
                    .idempotencyKey(idempotencyKey)
                    .state(PaymentState.PROCESSING)
                    .build();
            // DB에 INSERT, Unique 제약조건 예외 캐치
            idempotencyRepository.saveAndFlush(row);
            return true;
        } catch (DataIntegrityViolationException e) {
            // 이미 동일 키가 존재 → 중복 메시지
            return false;
        }
    }

    /**
     * 성공적으로 처리 완료했음을 기록.
     * (필요 없으면 no-op 구현해도 무방)
     */
    @Transactional
    public void markSuccess(String idempotencyKey) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(PaymentIdempotency::markDone); // save() 생략 권장
    }

}
