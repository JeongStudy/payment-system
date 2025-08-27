package com.system.payment.card.service;

import com.system.payment.card.domain.BillingKeyStatus;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.domain.PgCompany;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.response.PGAuthParamsResponse;
import com.system.payment.card.model.response.PaymentUserCardResponse;
import com.system.payment.card.provider.BillingAuthProviderRegistry;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final BillingAuthProviderRegistry registry;
    private final PaymentUserRepository paymentUserRepository;
    private final PaymentUserCardRepository paymentUserCardRepository;

    /**
     * user_id를 기준으로 카드 리스트 조회
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public List<PaymentUserCardResponse> getActiveCards(Integer userId) {
        List<PaymentUserCard> cards =
                paymentUserCardRepository.findByUser_IdAndIsDeletedFalseAndBillingKeyStatus(
                        userId, BillingKeyStatus.ACTIVE,
                        Sort.by(Sort.Direction.DESC, "createdTimestamp")
                );
        return PaymentUserCardResponse.from(cards);
    }

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
        String oid = IdGeneratorUtil.oidGenerate();

        savePendingCard(user, oid, pgCompany);

        return registry.get(pgCompany).createAuthParams(oid, request);
    }

    private void savePendingCard(PaymentUser user, String oid, PgCompany pgCompany) {
        paymentUserCardRepository.save(PaymentUserCard.builder()
                .user(user)
                .pgOid(oid)
                .pgCompany(pgCompany)
                .pgCompanyCode(pgCompany.getCode())
                .billingKeyStatus(BillingKeyStatus.PENDING)
                .build());
    }
}
