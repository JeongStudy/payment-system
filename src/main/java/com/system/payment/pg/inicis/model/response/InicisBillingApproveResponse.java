package com.system.payment.pg.inicis.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true) // 문서 외 필드 와도 안전
public record InicisBillingApproveResponse(
        String resultCode,      // 결과코드 ("00" 성공, 그 외 실패)
        String resultMsg,       // 결과메시지
        String payDate,         // 거래일자 [YYYYMMDD]
        String payTime,         // 거래시간 [hhmmss]
        @JsonProperty("payAuthCode")
        String payAuthCode,     // 승인번호
        String tid,             // 거래번호(TID)
        String price,           // 거래금액
        String cardCode,        // 카드코드
        String cardQuota,       // 할부개월
        String checkFlg,        // 카드구분 (0:신용,1:체크,2:기프트)
        String prtcCode,        // 부분환불 가능여부
        String usePoint,        // 포인트금액 (가맹점 전용)
        String cardPoint,       // 현대 mPoint 사용 거래
        String partnerDiscount, // 제휴코드
        String eventFlag,       // 이벤트 플래그
        String cardNumber       // 카드번호 (응답 여부 가맹점 설정에 따름)
) {
    /** 성공 여부 */
    public boolean isSuccess() {
        return "00".equals(resultCode);
    }

    /** payDate+payTime → LocalDateTime 변환 */
    public LocalDateTime approvedAt() {
        try {
            if (payDate == null || payTime == null || payDate.length() != 8 || payTime.length() != 6) {
                return null;
            }
            LocalDate d = LocalDate.of(
                    Integer.parseInt(payDate.substring(0, 4)),
                    Integer.parseInt(payDate.substring(4, 6)),
                    Integer.parseInt(payDate.substring(6, 8))
            );
            LocalTime t = LocalTime.of(
                    Integer.parseInt(payTime.substring(0, 2)),
                    Integer.parseInt(payTime.substring(2, 4)),
                    Integer.parseInt(payTime.substring(4, 6))
            );
            return LocalDateTime.of(d, t);
        } catch (Exception e) {
            return null;
        }
    }
}