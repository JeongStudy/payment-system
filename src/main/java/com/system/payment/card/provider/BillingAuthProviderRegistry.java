package com.system.payment.card.provider;

import com.system.payment.card.domain.PgCompany;
import com.system.payment.common.dto.response.ErrorCode;
import com.system.payment.common.exception.PaymentServerBadRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BillingAuthProviderRegistry {
    private final Map<PgCompany, BillingAuthProvider> providers;

    public BillingAuthProviderRegistry(List<BillingAuthProvider> beans) {
        Map<PgCompany, BillingAuthProvider> map = new EnumMap<>(PgCompany.class);
        for (BillingAuthProvider b : beans) {
            if (map.containsKey(b.supports())) {
                throw new IllegalStateException("Duplicate BillingAuthProvider for PG: " + b.supports());
            }
            map.put(b.supports(), b);
        }
        this.providers = Map.copyOf(map); // 불변화
    }

    public BillingAuthProvider get(PgCompany pg) {
        BillingAuthProvider p = providers.get(pg);
        if (p == null) throw new PaymentServerBadRequestException(ErrorCode.BAD_REQUEST_PARAM);
        return p;
    }
}