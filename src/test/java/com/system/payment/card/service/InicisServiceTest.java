package com.system.payment.card.service;

import com.system.payment.card.domain.BillingKeyStatus;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.request.InicisRequest;
import com.system.payment.card.model.response.InicisBillingAuthResponse;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.exception.PaymentServerNotFoundException;
import com.system.payment.pg.inicis.InicisClient;
import com.system.payment.util.HashUtils;
import com.system.payment.util.IdGeneratorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InicisServiceTest {

    private static final String VERSION = "1.0";
    private static final String PAY_METHOD = "CARD";
    private static final String MID = "INIBillTst";
    private static final String PRICE_ZERO = "0";
    private static final String USE_CHKFAKE = "Y";
    private static final String CURRENCY = "WON";
    private static final String CHARSET = "UTF-8";
    private static final String ACCEPT_METHOD = "below1000:va_receipt:centerCd(Y):BILLAUTH(Card)";
    private static final String SIGN_KEY = "SU5JTElURV9UUklQTEVERVNfS0VZU1RS"; // 샘플 테스트 키

    private static final String OID_NOT_EXIST = "OID-NOT-EXIST";
    private static final String OID_ACTIVE = "OID-1";
    private static final String OID_PG_FAIL = "OID-2";
    private static final String OID_SUCCESS = "OID-3";

    private static final String TS = "1712345678000";

    private static final String RESULT_SUCCESS = "0000";
    private static final String RESULT_SAMPLE_FAIL = "1234";

    private static final String MASKED_CARD = "************9999";
    private static final String CARD_CHECK_FLAG_Y = "Y";
    private static final String CARD_CODE_KB = "KB";
    private static final String BILLING_KEY_SAMPLE = "BILL-OK-123";

    private static final String AUTH_URL = "https://auth";
    private static final String AUTH_TOKEN = "TOKEN";

    private static final String BUYER_NAME = "홍길동";
    private static final String BUYER_TEL = "01012345678";
    private static final String BUYER_EMAIL = "hong@test.com";
    private static final String GOOD_NAME = "정기결제";

    @Mock
    InicisClient inicisClient;

    @Mock
    PaymentUserCardRepository paymentUserCardRepository;

    @InjectMocks
    InicisService inicisService;

    @Test
    @DisplayName("PG 팝업 파라미터 생성 - 해시/필드 값 모두 검증")
    void createBillingAuthParams_buildsParamsWithCorrectHashesAndFields() {
        // given
        CardAuthRequest req = new CardAuthRequest();
        set(req, "buyerName", BUYER_NAME);
        set(req, "buyerTel", BUYER_TEL);
        set(req, "buyerEmail", BUYER_EMAIL);
        set(req, "goodName", GOOD_NAME);

        // when
        try (MockedStatic<IdGeneratorUtils> mocked = mockStatic(IdGeneratorUtils.class)) {
            mocked.when(IdGeneratorUtils::timestampGenerate).thenReturn(TS);

            InicisBillingAuthResponse res = inicisService.createBillingAuthParams(OID_SUCCESS, req);

            // then: 고정 필드
            assertThat(res.getVersion()).isEqualTo(VERSION);
            assertThat(res.getGopaymethod()).isEqualTo(PAY_METHOD);
            assertThat(res.getMid()).isEqualTo(MID);
            assertThat(res.getOid()).isEqualTo(OID_SUCCESS);
            assertThat(res.getPrice()).isEqualTo(PRICE_ZERO);
            assertThat(res.getUse_chkfake()).isEqualTo(USE_CHKFAKE);
            assertThat(res.getCurrency()).isEqualTo(CURRENCY);
            assertThat(res.getCharset()).isEqualTo(CHARSET);
            assertThat(res.getAcceptmethod()).isEqualTo(ACCEPT_METHOD);

            // then: 요청자/상품
            assertThat(res.getBuyername()).isEqualTo(BUYER_NAME);
            assertThat(res.getBuyertel()).isEqualTo(BUYER_TEL);
            assertThat(res.getBuyeremail()).isEqualTo(BUYER_EMAIL);
            assertThat(res.getGoodname()).isEqualTo(GOOD_NAME);

            // then: 해시 검증 (서비스와 동일 공식)
            String sigText = String.format("oid=%s&price=%s&timestamp=%s", OID_SUCCESS, PRICE_ZERO, TS);
            String varText = String.format("oid=%s&price=%s&signKey=%s&timestamp=%s", OID_SUCCESS, PRICE_ZERO, SIGN_KEY, TS);
            assertThat(res.getTimestamp()).isEqualTo(TS);
            assertThat(res.getSignature()).isEqualTo(HashUtils.sha256(sigText));
            assertThat(res.getVerification()).isEqualTo(HashUtils.sha256(varText));
            assertThat(res.getMKey()).isEqualTo(HashUtils.sha256(SIGN_KEY));

            // then: URL들 존재
            assertThat(res.getReturnUrl()).isNotBlank();
            assertThat(res.getCloseUrl()).isNotBlank();
        }
    }

    // ====== createAndSaveBillingKey ======

    @Test
    @DisplayName("빌링키 발급 - 카드가 없으면 NOT_FOUND 예외")
    void createAndSaveBillingKey_whenCardNotFound_throwNotFound() {
        // given
        InicisRequest req = minimalInicisRequest(OID_NOT_EXIST);
        when(paymentUserCardRepository.findByPgOid(OID_NOT_EXIST)).thenReturn(Optional.empty());

        // expect
        assertThatThrownBy(() -> inicisService.createAndSaveBillingKey(req))
                .isInstanceOf(PaymentServerNotFoundException.class);
    }

    @Test
    @DisplayName("빌링키 발급 - 이미 ACTIVE면 멱등 성공(0000), 외부호출/업데이트 없음")
    void createAndSaveBillingKey_whenAlreadyActive_return0000_noUpdate() {
        // given
        InicisRequest req = minimalInicisRequest(OID_ACTIVE);
        PaymentUserCard card = mock(PaymentUserCard.class);
        when(card.getBillingKeyStatus()).thenReturn(BillingKeyStatus.ACTIVE);
        when(paymentUserCardRepository.findByPgOid(OID_ACTIVE)).thenReturn(Optional.of(card));

        // when
        String code = inicisService.createAndSaveBillingKey(req);

        // then
        assertThat(code).isEqualTo(RESULT_SUCCESS);
        verifyNoInteractions(inicisClient);
        verify(card, never()).updateInicisCard(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("빌링키 발급 - PG 실패 코드면 그대로 반환하고 업데이트 없음")
    void createAndSaveBillingKey_whenPgReturnsFailure_returnCode_noUpdate() {
        // given
        InicisRequest req = minimalInicisRequest(OID_PG_FAIL);

        PaymentUserCard card = mock(PaymentUserCard.class);
        when(card.getBillingKeyStatus()).thenReturn(BillingKeyStatus.PENDING);
        when(paymentUserCardRepository.findByPgOid(OID_PG_FAIL)).thenReturn(Optional.of(card));

        InicisBillingKeyResponse resp = new InicisBillingKeyResponse();
        set(resp, "resultCode", RESULT_SAMPLE_FAIL);
        when(inicisClient.requestBillingKey(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(resp);

        // when
        String code = inicisService.createAndSaveBillingKey(req);

        // then
        assertThat(code).isEqualTo(RESULT_SAMPLE_FAIL);
        verify(card, never()).updateInicisCard(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("빌링키 발급 - 성공 시 카드 업데이트 호출 및 0000 반환")
    void createAndSaveBillingKey_whenPgSuccess_updateCard_andReturn0000() {
        // given
        InicisRequest req = minimalInicisRequest(OID_SUCCESS);

        PaymentUserCard card = mock(PaymentUserCard.class);
        when(card.getBillingKeyStatus()).thenReturn(BillingKeyStatus.PENDING);
        when(paymentUserCardRepository.findByPgOid(OID_SUCCESS)).thenReturn(Optional.of(card));

        InicisBillingKeyResponse resp = new InicisBillingKeyResponse();
        set(resp, "resultCode", RESULT_SUCCESS);
        set(resp, "CARD_Num", MASKED_CARD);
        set(resp, "CARD_CheckFlag", CARD_CHECK_FLAG_Y);
        set(resp, "CARD_Code", CARD_CODE_KB);
        set(resp, "CARD_BillKey", BILLING_KEY_SAMPLE);

        when(inicisClient.requestBillingKey(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(resp);

        // when
        String code = inicisService.createAndSaveBillingKey(req);

        // then
        assertThat(code).isEqualTo(RESULT_SUCCESS);
        verify(card).updateInicisCard(
                eq(MASKED_CARD),
                eq(CARD_CHECK_FLAG_Y),
                eq(CARD_CODE_KB),
                eq(BILLING_KEY_SAMPLE),
                eq(BillingKeyStatus.ACTIVE)
        );
    }

    // ====== helpers ======

    private static InicisRequest minimalInicisRequest(String oid) {
        InicisRequest req = new InicisRequest();
        set(req, "orderNumber", oid);
        set(req, "authUrl", AUTH_URL);
        set(req, "mid", MID);
        set(req, "authToken", AUTH_TOKEN);
        return req;
    }

    private static void set(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
