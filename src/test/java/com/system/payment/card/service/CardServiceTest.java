package com.system.payment.card.service;

import com.system.payment.card.domain.BillingKeyStatus;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.card.domain.PgCompany;
import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.response.PGAuthParamsResponse;
import com.system.payment.card.model.response.PaymentUserCardResponse;
import com.system.payment.card.provider.BillingAuthProvider;
import com.system.payment.card.provider.BillingAuthProviderRegistry;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.util.IdGeneratorUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    private static final String CREATED_TIMESTAMP = "createdTimestamp";
    private static final Integer USER_ID = 1;
    private static final Integer CARD_ID = 1;
    private static final String OID = "OID-1";
    private static final String PG_INICIS = "INICIS";

    @Mock
    private BillingAuthProviderRegistry registry;
    @Mock
    private PaymentUserRepository paymentUserRepository;
    @Mock
    private PaymentUserCardRepository paymentUserCardRepository;
    @Mock
    private IdGeneratorUtil idGeneratorUtil;

    @Mock
    private PaymentUser user;
    @Mock
    private BillingAuthProvider provider;
    @Mock
    private PGAuthParamsResponse authParamsResponse;

    @InjectMocks
    private CardService cardService;

    @Test
    @DisplayName("사용자 카드 목록 조회 - 단일 값 매핑 + 정렬/호출 검증")
    void getActiveCards_success() {
        // given
        PaymentUserCard card = mock(PaymentUserCard.class);
        when(card.getId()).thenReturn(CARD_ID);

        when(paymentUserCardRepository.findByUser_IdAndIsDeletedFalseAndBillingKeyStatus(
                eq(USER_ID), eq(BillingKeyStatus.ACTIVE), any(Sort.class))
        ).thenReturn(List.of(card));

        // when
        List<PaymentUserCardResponse> result = cardService.getActiveCards(USER_ID);

        // then: 결과 개수/매핑 검증
        assertThat(result).hasSize(1);
        PaymentUserCardResponse response = result.getFirst();
        assertThat(response.getCardId()).isEqualTo(CARD_ID);

        // then: 호출/정렬 검증
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(paymentUserCardRepository).findByUser_IdAndIsDeletedFalseAndBillingKeyStatus(
                eq(USER_ID), eq(BillingKeyStatus.ACTIVE), sortCaptor.capture());

        Sort sort = sortCaptor.getValue();
        assertThat(sort).isEqualTo(Sort.by(Sort.Direction.DESC, CREATED_TIMESTAMP));

        verifyNoMoreInteractions(paymentUserCardRepository);
    }

    @Test
    @DisplayName("PG 파라미터 생성 및 사용자 정보 저장 - 성공")
    void getBillingAuthParams_success() {
        // given
        CardAuthRequest req = new CardAuthRequest();
        req.setPgCompany(PG_INICIS);

        when(paymentUserRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(idGeneratorUtil.oidGenerate()).thenReturn(OID);
        when(registry.get(PgCompany.INICIS)).thenReturn(provider);
        when(provider.createAuthParams(OID, req)).thenReturn(authParamsResponse);

        ArgumentCaptor<PaymentUserCard> cardCaptor = ArgumentCaptor.forClass(PaymentUserCard.class);
        when(paymentUserCardRepository.save(cardCaptor.capture())).thenAnswer(a -> a.getArgument(0));

        // when
        PGAuthParamsResponse response = cardService.getBillingAuthParams(USER_ID, req);

        // then
        assertThat(response).isSameAs(authParamsResponse);

        PaymentUserCard savedCard = cardCaptor.getValue();
        assertThat(savedCard.getUser()).isSameAs(user);
        assertThat(savedCard.getPgOid()).isEqualTo(OID);
        assertThat(savedCard.getPgCompany()).isEqualTo(PgCompany.INICIS);
        assertThat(savedCard.getPgCompanyCode()).isEqualTo(PgCompany.INICIS.getCode());
        assertThat(savedCard.getBillingKeyStatus()).isEqualTo(BillingKeyStatus.PENDING);

        verify(paymentUserRepository).getReferenceById(USER_ID);
        verify(idGeneratorUtil).oidGenerate();
        verify(paymentUserCardRepository).save(any(PaymentUserCard.class));
        verify(registry).get(PgCompany.INICIS);
        verify(provider).createAuthParams(OID, req);
        verifyNoMoreInteractions(paymentUserCardRepository, paymentUserRepository, registry, provider);
    }
}
