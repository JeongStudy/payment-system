package com.system.payment.card.service;

import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.response.InicisBillingAuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Service
public class InicisService {

    @Value("${pg.inicis.mid}")
    private String mid;

    @Value("${pg.inicis.signKey}")
    private String signKey;

    @Value("${pg.inicis.returnUrl}")
    private String returnUrl;

    @Value("${pg.inicis.closeUrl}")
    private String closeUrl;

    public InicisBillingAuthResponse createBillingAuthParams(String buyerName, String buyerTel, String buyerEmail, String goodName) {
        String version = "1.0";
        String gopaymethod = "";
        String oid = "ORDER_" + Instant.now().toEpochMilli(); // 주문번호 유니크 생성
        String price = "100"; // 카드등록 100원 (이니시스 정책)
        String timestamp = String.valueOf(System.currentTimeMillis());
        String useChkfake = "Y";
        String currency = "WON";
        String charset = "UTF-8";
        String acceptmethod = "BILLAUTH(Card):centerCd(Y)";
        String offerPeriod = ""; // 필요 없으면 빈값 처리

        // ※ 해시 생성 공식은 반드시 이니시스 문서 기준으로 맞춰야 함!
        String signature = createSHA256(oid + price + signKey + timestamp);
        String verification = createSHA256(oid + price + signKey + timestamp);
        String mKey = createSHA256(mid + signKey);

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

    private String createSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
