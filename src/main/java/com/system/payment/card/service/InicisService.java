package com.system.payment.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.model.request.InicisRequest;
import com.system.payment.card.model.response.InicisBillingAuthResponse;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.util.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class InicisService {

    private final RestTemplate restTemplate;

    // 데모 테스트 고정 파라미터
    private String mid = "INIBillTst";
    private String oid = "DemoTest_" + System.currentTimeMillis();
    private String price = "1000";
    private long timestamp = System.currentTimeMillis();
    private String returnUrl = "https://7f02cdc1b8c4.ngrok-free.app/api/payment/cards/inicis/return";
    private String closeUrl = "https://7f02cdc1b8c4.ngrok-free.app/card/return";
    private String signKey = "SU5JTElURV9UUklQTEVERVNfS0VZU1RS";
    private String version = "1.0";
    private String gopaymethod = "CARD";
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
                .use_chkfake(useChkfake)
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

    public InicisBillingKeyResponse createBillingKey(InicisRequest request) {
        String authUrl = request.getAuthUrl();
        log.info(authUrl);
        mid = request.getMid();
        String authToken = request.getAuthToken();
        timestamp = System.currentTimeMillis();

        String signatureText = String.format("authToken=%s&timestamp=%s", authToken, timestamp);
        String signature = HashUtils.sha256(signatureText);

        String verificationText = String.format("authToken=%s&signKey=%s&timestamp=%s", authToken, signKey, timestamp);
        String verification = HashUtils.sha256(verificationText);

        String format = "JSON";

        // Form 데이터 생성
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("mid", mid);
        params.add("authToken", authToken);
        params.add("timestamp", String.valueOf(timestamp));
        params.add("signature", signature);
        params.add("verification", verification);
        params.add("charset", charset);
        params.add("format", format);

        // 헤더 세팅
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Entity 조립
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        // POST 호출 (응답을 String으로 받음)
        ResponseEntity<String> response = restTemplate.postForEntity(
                authUrl,
                entity,
                String.class
        );

        // 응답 JSON → DTO 매핑
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            log.info(response.getBody());
            return objectMapper.readValue(response.getBody(), InicisBillingKeyResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("이니시스 빌링키 발급 응답 파싱 실패", e);
        }
    }
}
