package com.system.payment.card.service;

import com.system.payment.card.model.response.InicisBillingAuthResponse;
import com.system.payment.util.HashUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Service
public class InicisService {

    // 데모 테스트 고정 파라미터
    private String mid = "INIBillTst";
    private String oid = "DemoTest_1754028023130";
    private String price = "1000";
    private Long timestamp = 1754028821750L;
    private String returnUrl = "localhost:3000/inicis/return";
    private String closeUrl = "localhost:3000/inicis/close";
    private String signKey = "SU5JQklMTFRzdERlbW9LZXlmb3JUZXN0";
    private String version = "1.0";
    private String gopaymethod = "";
    private String useChkfake = "Y";
    private String currency = "WON";
    private String charset = "UTF-8";
    private String acceptmethod = "below1000:va_receipt:centerCd(Y):BILLAUTH(Card)";
    private String offerPeriod = ""; // 필요 없으면 빈값 처리

    public InicisBillingAuthResponse createBillingAuthParams(String buyerName, String buyerTel, String buyerEmail, String goodName) {
        // ※ 해시 생성 공식은 반드시 이니시스 문서 기준으로 맞춰야 함!
        String signatureText = String.format("oid=%s&price=%s&timestamp=%s", oid, price, timestamp);
        String signature = HashUtils.sha256(signatureText);

        String varificationText = String.format("oid=%s&price=%s&signKey=%s&timestamp=%s", oid, price, signKey, timestamp);
        String verification = HashUtils.sha256(varificationText);

        String mKey = HashUtils.sha256(signKey);

        return InicisBillingAuthResponse.builder()
                .version(version)
                .gopaymethod(gopaymethod)
                .mid(mid)
                .oid(oid)
                .price(price)
                .timestamp(timestamp)
                .useChkfake(useChkfake)
                .signature(signature)
                .verification(verification)
                .mKey(mKey)
                .offerPeriod(offerPeriod)
                .currency(currency)
                .charset(charset)
                .goodname(goodName)
                .buyername(buyerName)
                .buyertel(buyerTel)
                .buyeremail(buyerEmail)
                .returnUrl(returnUrl)
                .closeUrl(closeUrl)
                .acceptmethod(acceptmethod)
                .build();
    }
}
