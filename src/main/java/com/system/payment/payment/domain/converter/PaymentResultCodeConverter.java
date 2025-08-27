package com.system.payment.payment.domain.converter;

import com.system.payment.payment.domain.PaymentResultCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentResultCodeConverter implements AttributeConverter<PaymentResultCode, String> {
	@Override
	public String convertToDatabaseColumn(PaymentResultCode a) {
		return a == null ? null : a.getCode();
	}

	@Override
	public PaymentResultCode convertToEntityAttribute(String d) {
		return d == null ? null : PaymentResultCode.fromCode(d);
	}
}