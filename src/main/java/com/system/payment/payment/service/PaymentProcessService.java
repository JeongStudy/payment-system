package com.system.payment.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProcessService {

    private final InicisPgClientService inicisPgClientService;
    // TODO: PaymentRepository, PaymentHistoryRepository 등 필요 리포지토리 주입
    // TODO: 트랜잭션 경계 @Transactional 적용 위치 결정

    public void process(
            // TODO: 메시지 DTO 타입 (예: PaymentRequestedMessageV1<?> message)
            Object message
    ) {
        // TODO: 결제 엔티티 조회/잠금(필요 시)
        // TODO: 상태 전이(REQUESTED -> PROCESSING)
        // TODO: PG 요청 DTO 변환
        // inicisPgClientService.approve(request);
        // TODO: 응답에 따른 상태 전이(COMPLETED/FAILED), 히스토리 적재
        // TODO: 예외/재시도/알람/메트릭
    }
}
