package com.system.payment.payment.service;

import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerInternalServerErrorException;
import com.system.payment.exception.PaymentServerNotFoundException;
import com.system.payment.payment.model.dto.PaymentDetailItem;

import java.util.List;

public final class PaymentItemValidator {

	public static void validateAndVerifyTotal(
			List<PaymentDetailItem> items,
			int requestedAmount
	) {
		if (items == null || items.isEmpty()) {
			throw new PaymentServerNotFoundException(ErrorCode.PAYMENT_ITEMS_NOT_FOUND);
		}

		long total = 0L;

		for (PaymentDetailItem it : items) {
			total += it.getItemAmount();
		}

		if ((int) total != requestedAmount) {
			throw new PaymentServerInternalServerErrorException(ErrorCode.PAYMENT_INVALID_ITEM_SUM_AMOUNT_AND_TOTAL_AMOUNT);
		}
	}
}
