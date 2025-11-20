package com.system.payment.pg.inicis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.common.exception.ErrorCode;
import com.system.payment.common.exception.PgResponseParseException;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.InicisBillingApproveResponse;
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
public class InicisClient{

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public InicisBillingKeyResponse requestBillingKey(
            String authUrl, String mid, String authToken, String timestamp,
            String signature, String verification, String charset
    ) {
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

        // TODO: String이 아닌, InicisBillingKeyResponse로 변환
        ResponseEntity<String> res = restTemplate.postForEntity(
                authUrl, new HttpEntity<>(params, headers), String.class);

        try {
            log.info("[INICIS] billing-key raw: {}", res.getBody());
            return objectMapper.readValue(res.getBody(), InicisBillingKeyResponse.class);
        } catch (Exception e) {
            throw new PgResponseParseException(ErrorCode.PG_RESPONSE_PARSE_ERROR);
        }
    }

    public InicisBillingApproveResponse requestBillingApproval(InicisBillingApproval request) {
        // TODO: 현재 시뮬레이터로 처리하므로 사용하지 않은 메서드
        String url = "https://iniapi.inicis.com/v2/pg/billing";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<InicisBillingApproveResponse> res = restTemplate.postForEntity(
                url, new HttpEntity<>(request, headers), InicisBillingApproveResponse.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            throw new RuntimeException("INI http error: " + res.getStatusCode());
        }
        return res.getBody();
    }
}
