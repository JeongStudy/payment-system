package com.system.payment.card.service;

import com.system.payment.card.domain.BillingKeyStatus;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.request.InicisRequest;
import com.system.payment.card.model.response.InicisBillingAuthResponse;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerNotFoundException;
import com.system.payment.pg.inicis.InicisClient;
import com.system.payment.util.HashUtils;
import com.system.payment.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InicisService {

    private final InicisClient inicisClient;
    private final PaymentUserCardRepository paymentUserCardRepository;

    // 데모 테스트 고정 파라미터
    private final String price = "0";
    private final String returnUrl = "https://c5af73f84ead.ngrok-free.app/api/payment/cards/inicis/return";
    private final String closeUrl = "https://c5af73f84ead.ngrok-free.app/card/return";
    private final String signKey = "SU5JTElURV9UUklQTEVERVNfS0VZU1RS";
    private final String version = "1.0";
    private final String gopaymethod = "CARD";
    private final String useChkfake = "Y";
    private final String currency = "WON";
    private final String charset = "UTF-8";
    private final String acceptmethod = "below1000:va_receipt:centerCd(Y):BILLAUTH(Card)";
    private final String offerPeriod = ""; // 필요 없으면 빈값 처리
    private final String billingKeySuccessCode = "0000";

    /**
     * PG(이니시스) 팝업창 오픈 파라미터
     * @param oid
     * @param request
     * @return
     */
    public InicisBillingAuthResponse createBillingAuthParams(String oid, CardAuthRequest request){

        String mid = "INIBillTst";
        String buyerName = request.getBuyerName();
        String buyerTel = request.getBuyerTel();
        String buyerEmail = request.getBuyerEmail();
        String goodName = request.getGoodName();

        String timestamp = IdGeneratorUtil.timestampGenerate();
        String signatureText = String.format("oid=%s&price=%s&timestamp=%s", oid, price, timestamp);
        String signature = HashUtils.sha256(signatureText);

        String verificationText = String.format("oid=%s&price=%s&signKey=%s&timestamp=%s", oid, price, signKey, timestamp);
        String verification = HashUtils.sha256(verificationText);

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
     * @param request
     * @return
     */
    @Transactional
    public String createAndSaveBillingKey(InicisRequest request) {
        String oid = request.getOrderNumber();
        // 1) 락 걸고 조회 (동시성 안전)
        PaymentUserCard card = paymentUserCardRepository.findByPgOid(oid)
                .orElseThrow(() -> new PaymentServerNotFoundException(ErrorCode.NOT_FOUND));

        // 2) 이미 ACTIVE면 멱등 성공
        if (card.getBillingKeyStatus() == BillingKeyStatus.ACTIVE) {
            return billingKeySuccessCode; // 성공 코드 고정
        }

        String authUrl = request.getAuthUrl();
        String mid = request.getMid();
        String authToken = request.getAuthToken();
        String timestamp = IdGeneratorUtil.timestampGenerate();

        String signatureText = String.format("authToken=%s&timestamp=%s", authToken, timestamp);
        String signature = HashUtils.sha256(signatureText);

        String verificationText = String.format("authToken=%s&signKey=%s&timestamp=%s", authToken, signKey, timestamp);
        String verification = HashUtils.sha256(verificationText);

        // 3) PG사 API 호출 (빌링키 발급)
        InicisBillingKeyResponse response = inicisClient.requestBillingKey(
                authUrl,
                mid,
                authToken,
                String.valueOf(timestamp),
                signature,
                verification,
                charset
        );

        // 4) 실패 코드면 그대로 리턴
        if (!billingKeySuccessCode.equals(response.getResultCode())) {
            return response.getResultCode();
        }

        // 카드 정보 업데이트
        card.updateInicisCard(
                response.getCARD_Num(),
                response.getCARD_CheckFlag(),
                response.getCARD_Code(),
                response.getCARD_BillKey(),
                BillingKeyStatus.ACTIVE
        );

        return response.getResultCode();
    }
}
