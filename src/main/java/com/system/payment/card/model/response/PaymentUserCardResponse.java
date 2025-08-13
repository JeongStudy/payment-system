package com.system.payment.card.model.response;

import com.system.payment.card.domain.PaymentUserCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentUserCardResponse {
    private Integer cardId;
    private String cardNumberMasked;
    private String cardType;
    private String cardCompany;

    public static PaymentUserCardResponse from(PaymentUserCard paymentUserCard) {
        return PaymentUserCardResponse.builder()
                .cardId(paymentUserCard.getId())
                .cardNumberMasked(paymentUserCard.getCardNumberMasked())
                .cardType(paymentUserCard.getCardType())
                .cardCompany(paymentUserCard.getCardCompany())
                .build();
    }

    public static List<PaymentUserCardResponse> from(List<PaymentUserCard> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        return entities.stream()
                .map(PaymentUserCardResponse::from)
                .toList();
    }
}
