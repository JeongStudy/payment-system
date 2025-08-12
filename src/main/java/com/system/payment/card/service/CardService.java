package com.system.payment.card.service;

import com.system.payment.card.domain.BillingKeyStatus;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.request.InicisRequest;
import com.system.payment.card.model.response.InicisBillingAuthResponse;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.card.model.response.PGAuthParamsResponse;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.repository.PaymentUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final InicisService inicisService;

    private final PaymentUserRepository paymentUserRepository;

    private final PaymentUserCardRepository paymentUserCardRepository;

    /**
     * user, oid를 기준으로 billingKeyStatus를 PENDING으로 유저 카드 테이블 저장
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public PGAuthParamsResponse getBillingAuthParams(Integer userId,CardAuthRequest request) {
        PaymentUser user = paymentUserRepository.getReferenceById(userId);
        String oid = "DemoTest_" + System.currentTimeMillis();

        // PENDING 저장
        paymentUserCardRepository.save(PaymentUserCard.builder()
                .user(user)
                .oid(oid)
                .billingKeyStatus(BillingKeyStatus.PENDING)
                .build());

        // 요청에서 PG사 타입 추출 (예: "INICIS", "TOSS" 등)
        if ("INICIS".equalsIgnoreCase(request.getPgCompany())) {
            return inicisService.createBillingAuthParams(oid, request.getBuyerName(), request.getBuyerTel(), request.getBuyerEmail(), request.getGoodName());
        }
        throw new IllegalArgumentException("지원하지 않는 PG사");
    }

    /**
     * 이니시스 콜백 API 응답, 빌링키 발급 및 유저 카드 테이블 업데이트 
     * @param request
     * @return
     */
    public InicisBillingKeyResponse handleInicisCallback(InicisRequest request) {
        String oid = request.getOrderNumber();
        return inicisService.createAndSaveBillingKey(oid, request);
    }
}
