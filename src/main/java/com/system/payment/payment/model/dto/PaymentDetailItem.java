package com.system.payment.payment.model.dto;

import com.system.payment.payment.domain.ItemType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentDetailItem {
	private ItemType itemType;
	private Integer itemId;
	private Integer itemAmount;

	public static PaymentDetailItem product(Integer productId, int amount) {
		return PaymentDetailItem.builder()
				.itemType(ItemType.PRODUCT)
				.itemId(productId)
				.itemAmount(amount)
				.build();
	}

	public static PaymentDetailItem point(Integer historyId, int usedPoint) {
		return PaymentDetailItem.builder()
				.itemType(ItemType.POINT)
				.itemId(historyId)
				.itemAmount(-Math.abs(usedPoint))
				.build();
	}

	public static PaymentDetailItem coupon(Integer couponId, int discount) {
		return PaymentDetailItem.builder()
				.itemType(ItemType.COUPON)
				.itemId(couponId)
				.itemAmount(-Math.abs(discount))
				.build();
	}
}
