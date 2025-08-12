package com.system.payment.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class InicisClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public InicisBillingKeyResponse requestBillingKey(
            String authUrl,
            String mid,
            String authToken,
            String timestamp,
            String signature,
            String verification,
            String charset) {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("mid", mid);
        params.add("authToken", authToken);
        params.add("timestamp", timestamp);
        params.add("signature", signature);
        params.add("verification", verification);
        params.add("charset", charset);
        params.add("format", "JSON");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<String> response = restTemplate.postForEntity(
                authUrl, new HttpEntity<>(params, headers), String.class);

        try {
            log.info("INICIS billing-key raw: {}", response.getBody());
            return objectMapper.readValue(response.getBody(), InicisBillingKeyResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("이니시스 빌링키 발급 응답 파싱 실패", e);
        }
    }
}
