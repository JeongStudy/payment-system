package com.system.payment.pg.inicis.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.system.payment.pg.common.PGAuthParamsResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
public class InicisBillingAuthResponse implements PGAuthParamsResponse {
    private final String version;
    private final String gopaymethod;
    private final String mid;
    private final String oid;
    private final String price;
    private final String timestamp;
    private final String use_chkfake;
    private final String signature;
    private final String verification;
    @JsonProperty("mKey")
    private final String mKey;
    private final String offerPeriod;
    private final String charset;
    private final String currency;
    private final String goodname;
    private final String buyername;
    private final String buyertel;
    private final String buyeremail;
    private final String returnUrl;
    private final String closeUrl;
    private final String acceptmethod;
}

