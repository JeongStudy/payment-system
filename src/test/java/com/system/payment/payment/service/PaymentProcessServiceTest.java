package com.system.payment.payment.service;

import com.system.payment.common.dto.response.ErrorCode;
import com.system.payment.common.exception.PaymentValidationException;
import com.system.payment.common.exception.TransientPgException;
import com.system.payment.payment.domain.entity.Payment;
import com.system.payment.payment.domain.entity.PaymentDetail;
import com.system.payment.payment.domain.constant.PaymentResultCode;
import com.system.payment.pg.inicis.model.request.InicisBillingApproval;
import com.system.payment.pg.inicis.model.response.InicisBillingApproveResponse;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.pg.inicis.service.InicisPgClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessServiceTest {

    private static final Integer PAYMENT_ID = 1;
    private static final String TX_ID = "tx-1";
    private static final String TID = "tid-1";
    private static final String ERROR_CODE = "F001";
    private static final String ERROR_MSG = "한도초과";

    @Mock
    InicisPgClientService inicisPgClientService;

    @Mock
    PaymentHistoryService paymentHistoryService;

    @Mock
    PaymentRepository paymentRepository;

    @InjectMocks PaymentProcessService paymentProcessService;

    // 공용 메시지 모킹 유틸
    @SuppressWarnings("unchecked")
    private PaymentRequestedMessageV1<InicisBillingApproval> mockMessage (Integer paymentId, String txId) {
        var msg = mock(PaymentRequestedMessageV1.class);
        var payload = mock(PaymentRequestedMessageV1.Payload.class);
        var external = mock(PaymentRequestedMessageV1.Payload.External.class);
        var identifiers = mock(PaymentRequestedMessageV1.Identifiers.class);
        var approval = mock(InicisBillingApproval.class);

        when(msg.payload()).thenReturn(payload);
        when(payload.external()).thenReturn(external);
        when(external.approval()).thenReturn(approval);

        when(msg.identifiers()).thenReturn(identifiers);
        when(identifiers.paymentId()).thenReturn(paymentId);
        when(identifiers.transactionId()).thenReturn(txId);
        return msg;
    }

    @Test
    @DisplayName("메시지가 null이면 PaymentValidationException 발생")
    void 메시지가_null이면_예외발생() {
        assertThrows(PaymentValidationException.class, () -> paymentProcessService.process(null));
    }

    @Test
    @DisplayName("결제가 존재하지 않으면 PaymentValidationException 발생")
    void 결제가_존재하지않으면_예외발생() {
        // given
        PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(PAYMENT_ID, TX_ID);

        // when
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        // then
        assertThrows(PaymentValidationException.class, () -> paymentProcessService.process(msg));
    }

    @Test
    @DisplayName("이미 COMPLETED 상태면 아무 동작도 하지 않음")
    void 이미_완료상태면_아무것도_안함() {
        // given
        PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(PAYMENT_ID, TX_ID);
        Payment payment = mock(Payment.class);

        // when
        when(payment.getPaymentResultCode()).thenReturn(PaymentResultCode.COMPLETED);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        paymentProcessService.process(msg);

        // then
        verify(inicisPgClientService, never()).approve(any());
        verify(paymentHistoryService, never()).recordRequested(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 FAILED 상태면 아무 동작도 하지 않음")
    void 이미_FAILED상태면_아무것도_안함() {
        // given
        PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(PAYMENT_ID, TX_ID);
        Payment payment = mock(Payment.class);

        // when
        when(payment.getPaymentResultCode()).thenReturn(PaymentResultCode.FAILED);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        paymentProcessService.process(msg);

        // then
        verify(inicisPgClientService, never()).approve(any());
        verify(paymentHistoryService, never()).recordRequested(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("WAITING 상태에서 비즈 실패여도 REQUESTED 전이 수행")
    void WAITING에서_비즈실패여도_REQUESTED전이() {
        // given
        PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(PAYMENT_ID, TX_ID);
        Payment payment = mock(Payment.class);
        PaymentDetail d1 = mock(PaymentDetail.class);

        // when
        when(payment.getPaymentResultCode()).thenReturn(PaymentResultCode.WAITING);
        when(payment.getDetails()).thenReturn(List.of(d1));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        InicisBillingApproveResponse res = mock(InicisBillingApproveResponse.class);
        when(res.isSuccess()).thenReturn(false);
        when(res.resultCode()).thenReturn(ERROR_CODE);
        when(res.resultMsg()).thenReturn(ERROR_MSG);
        when(inicisPgClientService.approve(any())).thenReturn(res);

        paymentProcessService.process(msg);

        // then
        verify(payment).markRequested();      // 전이 수행 확인
        verify(d1).markFailed();                          // 실패 처리
        verify(paymentHistoryService).recordRequested(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PG 승인 응답이 실패이면 Payment/Detail FAILED 처리 후 History 저장")
    void PG승인응답_비즈니스실패이면_FAILED처리와_History저장() {
        // given
        PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(PAYMENT_ID, TX_ID);
        PaymentDetail d1 = mock(PaymentDetail.class);
        PaymentDetail d2 = mock(PaymentDetail.class);
        Payment payment = mock(Payment.class);

        // when
        when(payment.getPaymentResultCode()).thenReturn(PaymentResultCode.REQUESTED);
        when(payment.getDetails()).thenReturn(List.of(d1, d2));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        InicisBillingApproveResponse res = mock(InicisBillingApproveResponse.class);
        when(res.isSuccess()).thenReturn(false);
        when(res.resultCode()).thenReturn(ERROR_CODE);
        when(res.resultMsg()).thenReturn(ERROR_MSG);
        when(inicisPgClientService.approve(any())).thenReturn(res);

        paymentProcessService.process(msg);

        // then
        verify(d1).markFailed();
        verify(d2).markFailed();
        verify(payment).markFailed(eq(ERROR_CODE), eq(ERROR_MSG), any(LocalDateTime.class));
        verify(paymentHistoryService).recordFailed(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PG 승인 응답이 성공이면 Payment/Detail COMPLETED 처리 후 History 저장")
    void PG승인응답_성공이면_COMPLETED처리와_History저장() {
        // given
        PaymentRequestedMessageV1<InicisBillingApproval> msg = mockMessage(PAYMENT_ID, TX_ID);
        PaymentDetail d1 = mock(PaymentDetail.class);
        PaymentDetail d2 = mock(PaymentDetail.class);
        Payment payment = mock(Payment.class);

        // when
        when(payment.getPaymentResultCode()).thenReturn(PaymentResultCode.WAITING);
        when(payment.getDetails()).thenReturn(List.of(d1, d2));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        InicisBillingApproveResponse res = mock(InicisBillingApproveResponse.class);
        when(res.isSuccess()).thenReturn(true);
        when(res.tid()).thenReturn(TID);
        when(res.approvedAt()).thenReturn(LocalDateTime.now());
        when(inicisPgClientService.approve(any())).thenReturn(res);

        paymentProcessService.process(msg);

        // then
        verify(payment).markRequested();
        verify(d1).markCompleted();
        verify(d2).markCompleted();
        verify(payment).markCompleted(eq(TID), any(LocalDateTime.class));
        verify(paymentHistoryService).recordCompleted(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PG 통신 오류 발생 시 예외 전파되고 상태/History는 저장되지 않음")
    void PG통신오류가_발생하면_예외전파되고_History저장안됨() throws Exception {
        var msg = mockMessage(PAYMENT_ID, TX_ID);
        Payment payment = mock(Payment.class);

        when(payment.getPaymentResultCode()).thenReturn(PaymentResultCode.WAITING);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(inicisPgClientService.approve(any())).thenThrow(new TransientPgException(ErrorCode.PG_TIMEOUT));

        assertThrows(TransientPgException.class, () -> paymentProcessService.process(msg));

        verify(payment).markRequested();
        verify(paymentHistoryService).recordRequested(any(), any(), any(), any(),any(), any(), isNull());
        verify(payment, never()).markCompleted(anyString(), any());
        verify(payment, never()).markFailed(anyString(), anyString(), any());
        verify(paymentHistoryService, never()).recordFailed(any(), any(), any(), any(), any(), any(), any());
        verify(paymentHistoryService, never()).recordCompleted(any(), any(), any(), any(), any(), any(), any());
    }
}
