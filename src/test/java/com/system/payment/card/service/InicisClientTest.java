package com.system.payment.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.exception.PgResponseParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InicisClientTest {

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final InicisClient client = new InicisClient(restTemplate, objectMapper);

    private static final String AUTH_URL = "https://auth";
    private static final String MID = "INIBillTst";
    private static final String AUTH_TOKEN = "token-123";
    private static final String TIMESTAMP = "1712345678000";
    private static final String SIGNATURE = "sig";
    private static final String VERIFICATION = "ver";
    private static final String CHARSET = "UTF-8";

    private static final String SUCCESS_JSON = """
        {"resultCode":"0000"}
        """;
    private static final String FAIL_JSON = "{not-json}";

    @Test
    @DisplayName("성공: 2xx + JSON 파싱 + 폼 파라미터 검증(format=JSON 포함)")
    void requestBillingKey_success_and_form_verified() throws Exception {
        // given
        when(restTemplate.postForEntity(eq(AUTH_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(SUCCESS_JSON));

        InicisBillingKeyResponse mapped = new InicisBillingKeyResponse();
        var f = InicisBillingKeyResponse.class.getDeclaredField("resultCode");
        f.setAccessible(true); f.set(mapped, "0000");
        when(objectMapper.readValue(eq(SUCCESS_JSON), eq(InicisBillingKeyResponse.class))).thenReturn(mapped);

        // when
        InicisBillingKeyResponse res = client.requestBillingKey(AUTH_URL, MID, AUTH_TOKEN, TIMESTAMP, SIGNATURE, VERIFICATION, CHARSET);

        // then: 결과
        assertThat(res.getResultCode()).isEqualTo("0000");

        // then: 요청 폼 검증 (헤더 Accept 검증 없음)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> cap =
                ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).postForEntity(eq(AUTH_URL), cap.capture(), eq(String.class));
        HttpEntity<MultiValueMap<String, String>> entity = cap.getValue();

        MultiValueMap<String, String> form = entity.getBody();

        assertThat(form).isNotNull();
        assertThat(form.getFirst("mid")).isEqualTo(MID);
        assertThat(form.getFirst("authToken")).isEqualTo(AUTH_TOKEN);
        assertThat(form.getFirst("timestamp")).isEqualTo(TIMESTAMP);
        assertThat(form.getFirst("signature")).isEqualTo(SIGNATURE);
        assertThat(form.getFirst("verification")).isEqualTo(VERIFICATION);
        assertThat(form.getFirst("charset")).isEqualTo(CHARSET);
        assertThat(form.getFirst("format")).isEqualTo("JSON");
    }

    @Test
    @DisplayName("실패: JSON 파싱 예외 → PgResponseParseException")
    void requestBillingKey_parseError_throwsPgParseEx() throws Exception {

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(FAIL_JSON));
        when(objectMapper.readValue(eq(FAIL_JSON), eq(InicisBillingKeyResponse.class)))
                .thenThrow(new RuntimeException("parse"));

        assertThatThrownBy(() ->
                client.requestBillingKey(AUTH_URL, MID, AUTH_TOKEN, TIMESTAMP, SIGNATURE, VERIFICATION, CHARSET)
        ).isInstanceOf(PgResponseParseException.class);
    }

    @Test
    @DisplayName("실패: RestTemplate 예외는 그대로 전파(RestClientException)")
    void requestBillingKey_httpThrows_propagates() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("boom"));

        assertThatThrownBy(() ->
                client.requestBillingKey(AUTH_URL, MID, AUTH_TOKEN, TIMESTAMP, SIGNATURE, VERIFICATION, CHARSET)
        ).isInstanceOf(RestClientException.class);
    }
}
