package com.system.payment.payment.service;

import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.InicisBillingApproveResponse;
import com.system.payment.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
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

    public InicisBillingApproveResponse approve(InicisBillingApproval req) {
        Objects.requireNonNull(req, "approval request is null");
        InicisBillingApproval.Data data = Objects.requireNonNull(req.getData(), "approval data is null");

        String moid  = Objects.requireNonNullElse(data.getMoid(), "NA");
        String price = Objects.requireNonNullElse(data.getPrice(), "0");

        boolean fail = moid.contains("FAIL") || (!price.isEmpty() && price.charAt(price.length() - 1) == '9');

        LocalDateTime now = LocalDateTime.now();
        String payDate = "%04d%02d%02d".formatted(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String payTime = "%02d%02d%02d".formatted(now.getHour(), now.getMinute(), now.getSecond());

        if (fail) {
            log.warn("[SIM][APPROVAL][FAIL] moid={}, price={}", moid, price);
            return new InicisBillingApproveResponse(
                    "05",                               // 임의 실패 코드
                    "Simulator: approval rejected",     // 실패 메시지
                    payDate,
                    payTime,
                    null,                               // payAuthCode
                    "SIM-" + payDate + payTime,         // tid
                    price,
                    "CC01",                             // cardCode (예시)
                    "00",                               // cardQuota (일시불)
                    "0",                                // checkFlg (신용)
                    "0",                                // prtcCode (부분환불불가)
                    null, null, null, "0",
                    null
            );
        }

        String authCode = StringUtil.randomDigits(6);
        var res = new InicisBillingApproveResponse(
                "00",                                  // 성공
                "Simulator: approval ok",
                payDate,
                payTime,
                authCode,                              // payAuthCode
                "SIM-" + payDate + payTime + "-" + StringUtil.randomDigits(4), // tid
                price,
                "CC01",
                "00",
                "0",
                "1",                                   // prtcCode (부분환불가능)
                "0", null, null, "0",
                "4040-****-****-1234"
        );

        log.info("[SIM][APPROVAL][OK] moid={}, tid={}, auth={}", moid, res.tid(), res.payAuthCode());
        return res;
    }
}