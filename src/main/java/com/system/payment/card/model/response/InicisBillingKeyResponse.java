package com.system.payment.card.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class InicisBillingKeyResponse {
    @JsonProperty("CARD_Quota")
    private String CARD_Quota;

    private String buyerTel;
    private String parentEmail;
    private String applDate;
    private String buyerEmail;

    @JsonProperty("p_Sub")
    private String p_Sub;

    private String resultCode;

    private String mid;

    @JsonProperty("CARD_Num")
    private String CARD_Num;

    private String authSignature;

    private String tid;

    @JsonProperty("EventCode")
    private String EventCode;

    private String payMethodDetail;

    @JsonProperty("CARD_ApplNum")
    private String CARD_ApplNum;

    private String goodName;

    @JsonProperty("TotPrice")
    private String TotPrice;

    private String payMethod;

    @JsonProperty("CARD_MemberNum")
    private String CARD_MemberNum;

    @JsonProperty("MOID")
    private String MOID;

    private String currency;

    @JsonProperty("CARD_PurchaseCode")
    private String CARD_PurchaseCode;

    private String applTime;

    private String goodsName;

    @JsonProperty("CARD_CheckFlag")
    private String CARD_CheckFlag;

    @JsonProperty("FlgNotiSendChk")
    private String FlgNotiSendChk;

    @JsonProperty("CARD_Code")
    private String CARD_Code;

    @JsonProperty("CARD_TerminalNum")
    private String CARD_TerminalNum;

    @JsonProperty("CARD_BankCode")
    private String CARD_BankCode;

    private String buyerName;

    @JsonProperty("p_SubCnt")
    private String p_SubCnt;

    private String resultMsg;

    @JsonProperty("CARD_Interest")
    private String CARD_Interest;

    @JsonProperty("CARD_ApplPrice")
    private String CARD_ApplPrice;

    @JsonProperty("CARD_GWCode")
    private String CARD_GWCode;

    @JsonProperty("CARD_BillKey")
    private String CARD_BillKey;

    @JsonProperty("CARD_AuthType")
    private String CARD_AuthType;

    private String custEmail;

    private String payDevice;
}
