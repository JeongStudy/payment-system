package com.system.payment.pg.common;

import com.system.payment.card.model.request.CardAuthRequest;

public interface BillingAuthProvider {
    PgCompany supports(); // 자신이 담당하는 PG
    PGAuthParamsResponse createAuthParams(String oid, CardAuthRequest request);
}
