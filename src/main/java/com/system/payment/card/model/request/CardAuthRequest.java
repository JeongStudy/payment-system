package com.system.payment.card.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardAuthRequest{
    private String pgCompany;
    private String buyerName;
    private String buyerTel;
    private String buyerEmail;
    private String goodName;
}
