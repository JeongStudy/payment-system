package com.system.payment.card.model.request;

import lombok.Getter;

@Getter
public class CardAuthRequest{
    private String pgCompany;
    private String buyername;
    private String buyertel;
    private String buyeremail;
    private String goodname;
}
