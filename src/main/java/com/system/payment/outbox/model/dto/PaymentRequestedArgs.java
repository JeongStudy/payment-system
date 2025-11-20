package com.system.payment.outbox.model.dto;

public record PaymentRequestedArgs(
		Integer paymentId,
		String transactionId,
		Integer userId,
		String methodType,
		Integer methodId,
		String productName
) {
}
