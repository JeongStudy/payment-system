package com.system.payment.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.domain.BillingKeyStatus;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.request.InicisRequest;
import com.system.payment.card.model.response.InicisBillingAuthResponse;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.util.HashUtils;
import com.system.payment.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final InicisClient inicisClient;
    private final PaymentUserCardRepository paymentUserCardRepository;
    private final IdGeneratorUtil idGeneratorUtil;

    // 데모 테스트 고정 파라미터
    private String mid = "INIBillTst";
    private String price = "0";
    private String returnUrl = "https://c5af73f84ead.ngrok-free.app/api/payment/cards/inicis/return";
    private String closeUrl = "https://c5af73f84ead.ngrok-free.app/card/return";
    private String signKey = "SU5JTElURV9UUklQTEVERVNfS0VZU1RS";
    private String version = "1.0";
    private String gopaymethod = "CARD";
    private String useChkfake = "Y";
    private String currency = "WON";
    private String charset = "UTF-8";
    private String acceptmethod = "below1000:va_receipt:centerCd(Y):BILLAUTH(Card)";
    private String offerPeriod = ""; // 필요 없으면 빈값 처리

    /**
     * PG(이니시스) 팝업창 오픈 파라미터
     * @param oid
     * @param request
     * @return
     */
    public InicisBillingAuthResponse createBillingAuthParams(String oid, CardAuthRequest request){

        String buyerName = request.getBuyerName();
        String buyerTel = request.getBuyerTel();
        String buyerEmail = request.getBuyerEmail();
        String goodName = request.getGoodName();

        // ※ 해시 생성 공식은 반드시 이니시스 문서 기준으로 맞춰야 함!
        String timestamp = idGeneratorUtil.timestampGenerate();
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

    /**
     * 빌링키 발급 요청(외부) 및 유저 카드 업데이트
     * @param oid
     * @param request
     * @return
     */
    @Transactional
    public InicisBillingKeyResponse createAndSaveBillingKey(String oid, InicisRequest request) {
        // 1) 사전등록 행 조회 (PENDING)
        PaymentUserCard userCard = paymentUserCardRepository.findByOid(oid)
                .orElseThrow(() -> new IllegalStateException("사전등록 카드가 없습니다. oid=" + oid));

        if (userCard.getBillingKeyStatus() == BillingKeyStatus.ACTIVE) {
            // 이미 처리됨 – 멱등
            log.info("이미 ACTIVE. oid={}", oid);
            return null;
        }

        String authUrl = request.getAuthUrl();
        mid = request.getMid();
        String authToken = request.getAuthToken();
        String timestamp = idGeneratorUtil.timestampGenerate();

        String signatureText = String.format("authToken=%s&timestamp=%s", authToken, timestamp);
        String signature = HashUtils.sha256(signatureText);

        String verificationText = String.format("authToken=%s&signKey=%s&timestamp=%s", authToken, signKey, timestamp);
        String verification = HashUtils.sha256(verificationText);

        InicisBillingKeyResponse response = inicisClient.requestBillingKey(
                authUrl,
                mid,
                authToken,
                String.valueOf(timestamp),
                signature,
                verification,
                charset
        );

        // 카드 정보 업데이트
        userCard.updateInicisCard(
                response.getCARD_Num(),
                response.getCARD_CheckFlag(),
                response.getCARD_Code(),
                response.getCARD_BillKey(),
                BillingKeyStatus.ACTIVE
        );

        return response;
    }
}
