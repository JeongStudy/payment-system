package com.system.payment.payment.domain.outbox;

public record PaymentRequestedArgs(
		Integer paymentId, String transactionId, Integer userId, String methodType, Integer methodId, String productName
) {
}
