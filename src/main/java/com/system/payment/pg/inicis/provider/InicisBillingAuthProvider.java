package com.system.payment.pg.inicis.provider;

import com.system.payment.pg.common.PgCompany;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.pg.common.PGAuthParamsResponse;
import com.system.payment.pg.common.BillingAuthProvider;
import com.system.payment.pg.inicis.service.InicisService;
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
