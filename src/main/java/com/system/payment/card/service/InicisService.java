package com.system.payment.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.domain.BillingKeyStatus;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.model.request.InicisRequest;
import com.system.payment.card.model.response.InicisBillingAuthResponse;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.util.HashUtils;
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

    // 데모 테스트 고정 파라미터
    private String mid = "INIBillTst";
    private String price = "1000";
    private long timestamp = System.currentTimeMillis();
    private String returnUrl = "https://7a3af19f4d4c.ngrok-free.app/api/payment/cards/inicis/return";
    private String closeUrl = "https://7a3af19f4d4c.ngrok-free.app/card/return";
    private String signKey = "SU5JTElURV9UUklQTEVERVNfS0VZU1RS";
    private String version = "1.0";
    private String gopaymethod = "CARD";
    private String useChkfake = "Y";
    private String currency = "WON";
    private String charset = "UTF-8";
    private String acceptmethod = "below1000:va_receipt:centerCd(Y):BILLAUTH(Card)";
    private String offerPeriod = ""; // 필요 없으면 빈값 처리

    /**
     * PG 팝업창 오픈 파라미터 생성
     * @param oid
     * @param buyerName
     * @param buyerTel
     * @param buyerEmail
     * @param goodName
     * @return
     */
    public InicisBillingAuthResponse createBillingAuthParams(String oid, String buyerName, String buyerTel, String buyerEmail, String goodName) {
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
            // 이미 저장된 데이터를 기반으로 응답을 재구성할 수도 있으나,
            // 여기선 외부 응답을 그대로 반환하기 어렵기 때문에 최소 메시지 로그로 마무리.
            // 필요 시 PaymentUserCard → Response 매핑 추가.
//            return InicisBillingKeyResponse.builder()
//                    .mid(props.getMid())
//                    .MOID(oid)
//                    .CARD_BillKey(pending.getBillingKey())
//                    .CARD_Num(pending.getCardNumberMasked())
//                    .CARD_PurchaseCode(pending.getCardCompany())
//                    .payMethodDetail(pending.getCardType())
//                    .build();
        }

        String authUrl = request.getAuthUrl();
        log.info(authUrl);
        mid = request.getMid();
        String authToken = request.getAuthToken();
        timestamp = System.currentTimeMillis();

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
