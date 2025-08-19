package com.system.payment.card.provider;

import com.system.payment.card.domain.PgCompany;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.response.PGAuthParamsResponse;
import com.system.payment.card.service.InicisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InicisBillingAuthProvider implements BillingAuthProvider {

    private final InicisService inicisService;

    @Override
    public PgCompany supports() {
        return PgCompany.INICIS;
    }

    @Override
    public PGAuthParamsResponse createAuthParams(String oid, CardAuthRequest request) {
        return inicisService.createBillingAuthParams(oid, request);
    }
}
