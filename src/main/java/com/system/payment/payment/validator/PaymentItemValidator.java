package com.system.payment.payment.validator;

import com.system.payment.common.exception.ErrorCode;
import com.system.payment.common.exception.PaymentServerInternalServerErrorException;
import com.system.payment.common.exception.PaymentServerNotFoundException;
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
		int total = items.stream().mapToInt(PaymentDetailItem::getItemAmount).sum();
		if (total != requestedAmount) {
			throw new PaymentServerInternalServerErrorException(ErrorCode.PAYMENT_INVALID_ITEM_SUM_AMOUNT_AND_TOTAL_AMOUNT);
		}
	}
}
