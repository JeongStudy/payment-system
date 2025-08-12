package com.system.payment.payment.domain;

import com.system.payment.common.domain.BaseEntity;
import com.system.payment.payment.domain.converter.PaymentResultCodeConverter;
import com.system.payment.user.domain.PaymentUser;
import jakarta.persistence.*;
import lombok.*;
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

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	@Embedded
	private ItemRef itemRef;

	@Column(nullable = false)
	private Integer amount;

	@Convert(converter = PaymentResultCodeConverter.class)
	@Column(nullable = false, length = 2)
	private PaymentResultCode paymentDetailResultCode;


    public void markCompleted() { this.paymentDetailResultCode = PaymentResultCode.COMPLETED; }
    public void markFailed()    { this.paymentDetailResultCode = PaymentResultCode.FAILED; }
    public void markCanceled()  { this.paymentDetailResultCode = PaymentResultCode.CANCELED; }
}