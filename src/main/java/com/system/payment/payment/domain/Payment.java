package com.system.payment.payment.domain;

import com.system.payment.common.domain.BaseEntity;
import com.system.payment.exception.ErrorCode;
import com.system.payment.exception.PaymentServerBadRequestException;
import com.system.payment.payment.domain.converter.PaymentResultCodeConverter;
import com.system.payment.payment.model.dto.PaymentDetailItem;
import com.system.payment.payment.validator.PaymentItemValidator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment", schema = "payment")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Payment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Embedded
	private PaymentUserRef userRef;

	@Embedded
	private ReferenceRef referenceRef;

	@Embedded
	private PaymentMethodRef methodRef;

	@Enumerated(EnumType.STRING)
	@Column(length = 30, nullable = false)
	private PaymentType paymentType;

	@Column(nullable = false)
	private Integer totalAmount;

	@Convert(converter = PaymentResultCodeConverter.class)
	@Column(nullable = false, length = 2)
	private PaymentResultCode paymentResultCode;

	@Column
	private LocalDateTime requestedTimestamp;

	@Column
	private LocalDateTime approvedTimestamp;

	@Column
	private LocalDateTime canceledTimestamp;

	@Column
	private LocalDateTime failedTimestamp;

	@Column(length = 200)
	private String externalPaymentId;

	@Column(length = 20)
	private String errorCode;

	@Column(length = 300)
	private String errorMessage;

	@Column(length = 100, nullable = false, unique = true)
	private String idempotencyKey;

	@Column(length = 100, nullable = false, unique = true)
	private String transactionId;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "payment_id", nullable = false)
	@OrderBy("id ASC")
	private final List<PaymentDetail> details = new ArrayList<>();

	private Payment(PaymentUserRef userRef,
					ReferenceRef referenceRef,
					PaymentMethodRef methodRef,
					PaymentType paymentType,
					int totalAmount,
					PaymentResultCode paymentResultCode,
					String idempotencyKey,
					String transactionId) {
		this.userRef = userRef;
		this.referenceRef = referenceRef;
		this.methodRef = methodRef;
		this.paymentType = paymentType;
		this.totalAmount = totalAmount;
		this.paymentResultCode = paymentResultCode;
		this.idempotencyKey = idempotencyKey;
		this.transactionId = transactionId;
	}


	public static Payment create(PaymentUserRef userRef,
								 ReferenceRef referenceRef,
								 PaymentMethodRef methodRef,
								 PaymentType paymentType,
								 int totalAmount,
								 String idempotencyKey,
								 String transactionId,
								 List<PaymentDetailItem> items) {

		if (totalAmount <= 0)
			throw new PaymentServerBadRequestException(ErrorCode.PAYMENT_TOTAL_AMOUNT_MUST_BE_POSITIVE);
		PaymentItemValidator.validateAndVerifyTotal(items, totalAmount);
		PaymentResultCode paymentResultCode = PaymentResultCode.WAITING;
		Payment payment = new Payment(userRef, referenceRef, methodRef, paymentType, totalAmount, paymentResultCode, idempotencyKey, transactionId);
		payment.addDetails(items);
		return payment;
	}

	private void addDetails(List<PaymentDetailItem> itemList) {
		itemList.stream()
				.map(item -> PaymentDetail.create(ItemRef.of(item.getItemId(), item.getItemType()), item.getItemAmount()))
				.forEach(this.details::add);
	}

	public void changeResultCodeRequested() {
		this.paymentResultCode = PaymentResultCode.REQUESTED;
		this.requestedTimestamp = LocalDateTime.now();
	}
}
