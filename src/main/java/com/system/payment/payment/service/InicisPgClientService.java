package com.system.payment.payment.service;

import org.springframework.stereotype.Service;

@Service
public class InicisPgClientService {

    // TODO: RestTemplate/WebClient 주입
    // TODO: @ConfigurationProperties 로 mid/apiKey/url 등 주입
    // TODO: 시뮬레이터 분기(@Profile or 설정 플래그) 추가

    public void approve(
            // TODO: PG 승인 요청 DTO (예: PgApprovalRequest req)
    ) {
        // TODO: 실제 INICIS 호출 또는 시뮬레이터 응답 생성
        // TODO: 성공/실패 응답 DTO 반환하도록 시그니처 결정 (void → PgApprovalResponse)
        // TODO: 예외 매핑, 재시도/백오프 전략, 로깅/메트릭
    }
}