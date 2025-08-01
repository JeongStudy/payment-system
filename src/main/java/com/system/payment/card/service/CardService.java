package com.system.payment.card.service;

import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.response.InicisBillingAuthResponse;
import com.system.payment.card.model.response.PGAuthParamsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CardService {

    private final InicisService inicisService;

    public PGAuthParamsResponse getBillingAuthParams(CardAuthRequest request) {
        // 요청에서 PG사 타입 추출 (예: "INICIS", "TOSS" 등)
        String pgType = request.getPgCompany();
        if ("INICIS".equalsIgnoreCase(pgType)) {
            return inicisService.createBillingAuthParams(request.getBuyerName(), request.getBuyerTel(), request.getBuyerEmail(), request.getGoodName());
        }
        throw new IllegalArgumentException("지원하지 않는 PG사");
    }

}
