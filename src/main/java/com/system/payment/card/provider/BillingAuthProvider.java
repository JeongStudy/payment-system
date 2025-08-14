package com.system.payment.card.provider;

import com.system.payment.card.domain.PgCompany;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.response.PGAuthParamsResponse;

public interface BillingAuthProvider {
    PgCompany supports(); // 자신이 담당하는 PG
    PGAuthParamsResponse createAuthParams(String oid, CardAuthRequest request);
}
