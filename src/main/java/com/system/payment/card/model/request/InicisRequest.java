package com.system.payment.card.model.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class InicisRequest {
    private String resultCode;
    private String resultMsg;
    private String returnUrl;
    private String charset;
    private String orderNumber;
    private String authToken;
    private String checkAckUrl;
    private String netCancelUrl;
    private String mid;
    private String idc_name;
    private String merchantData;
    private String authUrl;
    private String cardnum;
}
