package com.system.payment.card.service;

import com.system.payment.card.domain.BillingKeyStatus;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.domain.PgCompany;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.request.InicisRequest;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.card.model.response.PGAuthParamsResponse;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardService {

    private final InicisService inicisService;
    private final PaymentUserRepository paymentUserRepository;
    private final PaymentUserCardRepository paymentUserCardRepository;
    private final IdGeneratorUtil idGeneratorUtil;

    /**
     * user, oid를 기준으로 billingKeyStatus를 PENDING으로 유저 카드 테이블 저장
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public PGAuthParamsResponse getBillingAuthParams(Integer userId,CardAuthRequest request) {
        PaymentUser user = paymentUserRepository.getReferenceById(userId);
        PgCompany pgCompany = PgCompany.from(request.getPgCompany());
        String oid = idGeneratorUtil.oidGenerate();

        savePendingCard(user, oid, pgCompany);

        if (pgCompany == PgCompany.INICIS) {
            return inicisService.createBillingAuthParams(oid, request);
        }
        throw new IllegalArgumentException("지원하지 않는 PG사");
    }

    private void savePendingCard(PaymentUser user, String oid, PgCompany pgCompany) {
        paymentUserCardRepository.save(PaymentUserCard.builder()
                .user(user)
                .oid(oid)
                .pgCompany(pgCompany)
                .pgCompanyCode(pgCompany.getCode())
                .billingKeyStatus(BillingKeyStatus.PENDING)
                .build());
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
