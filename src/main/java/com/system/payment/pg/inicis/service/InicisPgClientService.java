package com.system.payment.pg.inicis.service;

import com.system.payment.common.exception.TransientPgException;
import com.system.payment.pg.inicis.model.request.InicisBillingApproval;
import com.system.payment.pg.inicis.model.response.InicisBillingApproveResponse;
import com.system.payment.common.util.StringUtils;
import com.system.payment.pg.inicis.client.InicisClient;
import com.system.payment.pg.inicis.domain.constant.PgSimErrorTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class InicisPgClientService {

    private final InicisClient inicisClient;
    private final boolean useSimulator = true; // 운영도 시뮬레이터라면 true 고정

    public InicisBillingApproveResponse approve(InicisBillingApproval req) {
        if (useSimulator) {
            return simulate(req); // 시뮬레이터 로직
        }
        return inicisClient.requestBillingApproval(req); // 실제 PG 호출

    }

    /*
        빌링 승인 PG 시뮬레이터
     */
    private InicisBillingApproveResponse simulate(InicisBillingApproval req){
        Objects.requireNonNull(req, "approval request is null");
        InicisBillingApproval.Data data = Objects.requireNonNull(req.getData(), "approval data is null");

        String moid  = Objects.requireNonNullElse(data.getMoid(), "NA");
        String price = Objects.requireNonNullElse(data.getPrice(), "0");

        // ===== 장애 주입 트리거 (운영 포함, 프로파일 없이 항상 사용 가능) =====
        for (PgSimErrorTrigger trigger : PgSimErrorTrigger.values()) {
            if (moid.contains(trigger.name())) throw new TransientPgException(trigger.code);
        }
        // ===============================================================

        // ---- 기존 비즈니스 실패/성공 분기 ----
        boolean fail = moid.contains("FAIL");

        LocalDateTime now = LocalDateTime.now();
        String payDate = "%04d%02d%02d".formatted(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String payTime = "%02d%02d%02d".formatted(now.getHour(), now.getMinute(), now.getSecond());

        if (fail) {
            log.warn("[SIM][APPROVAL][FAIL] moid={}, price={}", moid, price);
            return new InicisBillingApproveResponse(
                    "05", "Simulator: approval rejected",
                    payDate, payTime, null, "SIM-" + payDate + payTime,
                    price, "CC01", "00", "0", "0", null, null, null, "0", null
            );
        }

        String authCode = StringUtils.randomDigits(6);
        var res = new InicisBillingApproveResponse(
                "00", "Simulator: approval ok",
                payDate, payTime, authCode,
                "SIM-" + payDate + payTime + "-" + StringUtils.randomDigits(4),
                price, "CC01", "00", "0", "1", "0", null, null, "0", "4040-****-****-1234"
        );

        log.info("[SIM][APPROVAL][OK] moid={}, tid={}, auth={}", moid, res.tid(), res.payAuthCode());
        return res;
    }
}
