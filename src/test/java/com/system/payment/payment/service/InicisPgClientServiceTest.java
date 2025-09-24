package com.system.payment.payment.service;

import com.system.payment.exception.TransientPgException;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.InicisBillingApproveResponse;
import com.system.payment.pg.inicis.InicisClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InicisPgClientServiceTest {

    private static final String URL = "https://billing";
    private static final String MID = "INIBillTst";
    private static final String GOOD_NAME = "상품명";
    private static final String BUYER_NAME = "홍길동";
    private static final String BUYER_EMAIL = "buyer@test.com";
    private static final String BUYER_TEL = "010-1234-5678";
    private static final String BILL_KEY = "BILL-KEY-123";
    private static final String TYPE = "Billing";
    private static final String PAYMETHOD = "Card";
    private static final String TIMESTAMP = "20250902123045";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String HASHED_DATA = "HASHED_DATA";

    private static final String PRICE = "1000";
    private static final String TIMEOUT_MOID = "ORDER-TIMEOUT-001";
    private static final String CONN_RESET_MOID = "PAY-CONN_RESET-X";
    private static final String HTTP_503_MOID = "X-HTTP_503-Y";
    private static final String JSON_ERROR_MOID = "TEST-JSON_ERROR";
    private static final String FAIL_MOID = "ANY-FAIL-CASE";
    private static final String SUCCESS_MOID = "OK-001";
    private static final String SUCCESS_CODE = "00";
    private static final String FAIL_CODE = "05";

    @Mock
    InicisClient inicisClient;

    @InjectMocks
    InicisPgClientService inicisPgClientService;

    private static InicisBillingApproval approval(String moid, String price) {
        InicisBillingApproval.Data data = InicisBillingApproval.Data.create(
                URL,
                moid,
                GOOD_NAME,
                BUYER_NAME,
                BUYER_EMAIL,
                BUYER_TEL,
                price,
                BILL_KEY
        );
        return InicisBillingApproval.create(
                MID,
                TYPE,
                PAYMETHOD,
                TIMESTAMP,
                CLIENT_IP,
                HASHED_DATA,
                data
        );
    }

    @Test
    @DisplayName("장애: moid에 TIMEOUT 포함 → TransientPgException")
    void 장애_TIMEOUT_예외() {
        assertThrows(TransientPgException.class,
                () -> inicisPgClientService.approve(approval(TIMEOUT_MOID, PRICE)));
    }

    @Test
    @DisplayName("장애: moid에 CONN_RESET 포함 → TransientPgException")
    void 장애_CONN_RESET_예외() {
        assertThrows(TransientPgException.class,
                () -> inicisPgClientService.approve(approval(CONN_RESET_MOID, PRICE)));
    }

    @Test
    @DisplayName("장애: moid에 HTTP_503 포함 → TransientPgException")
    void 장애_HTTP_503_예외() {
        assertThrows(TransientPgException.class,
                () -> inicisPgClientService.approve(approval(HTTP_503_MOID, PRICE)));
    }

    @Test
    @DisplayName("장애: moid에 JSON_ERROR 포함 → TransientPgException")
    void 장애_JSON_ERROR_예외() {
        assertThrows(TransientPgException.class,
                () -> inicisPgClientService.approve(approval(JSON_ERROR_MOID, PRICE)));
    }

    // 비즈니스 실패 - 응답 성공이나 isSuccess=false
    @Test @DisplayName("비즈니스 실패: moid에 FAIL 포함 → isSuccess=false, resultCode=05")
    void 비즈니스실패_moid_FAIL_응답_isSuccess_false() {
        InicisBillingApproveResponse res = inicisPgClientService.approve(approval(FAIL_MOID, PRICE));
        assertNotNull(res);
        assertFalse(res.isSuccess());
        assertEquals(FAIL_CODE, res.resultCode());
    }

    // 성공 시나리오
    @Test
    @DisplayName("성공: moid=OK, price=1000 → isSuccess=true, resultCode=00, tid/인증코드 존재")
    void 성공_OK_1000_isSuccess_true() {
        InicisBillingApproveResponse res = inicisPgClientService.approve(approval(SUCCESS_MOID, PRICE));
        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertEquals(SUCCESS_CODE, res.resultCode());
        assertNotNull(res.tid());
        assertNotNull(res.payAuthCode());
    }

    // 널 가드
    @Test
    @DisplayName("널 가드: approval=null → NPE")
    void 널가드_approval_null이면_예외() {
        assertThrows(NullPointerException.class, () -> inicisPgClientService.approve(null));
    }

    @Test
    @DisplayName("널 가드: approval.data=null → NPE")
    void 널가드_approval_data_null이면_예외() {
        var a = InicisBillingApproval.create(MID, BILL_KEY, PAYMETHOD, TIMESTAMP,
                CLIENT_IP, HASHED_DATA, null);
        assertThrows(NullPointerException.class, () -> inicisPgClientService.approve(a));
    }
}