package com.system.payment.payment.domain;

import com.system.payment.common.domain.BaseEntity;
import com.system.payment.payment.domain.converter.PaymentResultCodeConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "payment_detail", schema = "payment")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PaymentDetail extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Embedded
	private ItemRef itemRef;

	@Column(nullable = false)
	private Integer amount;

	@Convert(converter = PaymentResultCodeConverter.class)
	@Column(nullable = false, length = 2)
	private PaymentResultCode paymentDetailResultCode;

	private PaymentDetail(ItemRef itemRef, Integer amount, PaymentResultCode paymentDetailResultCode){
		this.itemRef = itemRef;
		this.amount = amount;
		this.paymentDetailResultCode = paymentDetailResultCode;
	}

	public static PaymentDetail create(ItemRef itemRef, Integer amount){
		return new PaymentDetail(
				itemRef,
				amount,
				PaymentResultCode.WAITING
		);
	}

    public void markCompleted() { this.paymentDetailResultCode = PaymentResultCode.COMPLETED; }
    public void markFailed()    { this.paymentDetailResultCode = PaymentResultCode.FAILED; }
    public void markCanceled()  { this.paymentDetailResultCode = PaymentResultCode.CANCELED; }
}