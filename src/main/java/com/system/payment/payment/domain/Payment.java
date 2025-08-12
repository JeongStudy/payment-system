package com.system.payment.payment.domain;

import com.system.payment.common.domain.BaseEntity;
import com.system.payment.payment.domain.converter.PaymentResultCodeConverter;
import com.system.payment.user.domain.PaymentUser;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Ref;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.time.LocalDateTime.now;

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

	@Column(nullable = false)
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

	@Column(length = 100, nullable = false)
	private String idempotencyKey;

	@Column(length = 100, nullable = false)
	private String transactionId;

	@OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private final List<PaymentDetail> details = new ArrayList<>();

//	public PaymentDetail addDetail(Integer itemId, ItemType itemType, Integer amount) {
//		PaymentDetail detail = PaymentDetail.builder()
//        .payment(this)
//        .itemRef(ItemRef.of(itemId, itemType))
//        .amount(amount)
//        .paymentDetailResultCode(PaymentResultCode.WAITING) // 컨버터가 "00" 저장
//        .build();
//		this.details.add(detail);
//    	return detail;
//	}
//
//	public void removeDetail(PaymentDetail detail) {
//		this.details.remove(detail); // orphanRemoval=true → 자동 삭제
//	}
//
//	public void markRequested() {
//		this.paymentResultCode = PaymentResultCode.REQUESTED;
//		this.requestedTimestamp = LocalDateTime.now();
//	}
//
//	public void markCompleted() {
//		this.paymentResultCode = PaymentResultCode.COMPLETED;
//		this.approvedTimestamp = LocalDateTime.now();
//		// (옵션) 총액 검증: sum(details.amount) == totalAmount
//		// validateTotals();
//	}
//
//	public void markFailed(String code, String message) {
//		this.paymentResultCode = PaymentResultCode.FAILED;
//		this.errorCode = code;
//		this.errorMessage = message;
//		this.failedTimestamp = LocalDateTime.now();
//	}
//
//	public void markCanceled(String reason) {
//		this.paymentResultCode = PaymentResultCode.CANCELED;
//		this.canceledTimestamp = LocalDateTime.now();
//		this.errorMessage = reason;
//	}
//
//	private void validateTotals() {
//		int sum = details.stream().mapToInt(PaymentDetail::getAmount).sum();
//		if (!Objects.equals(sum, this.totalAmount)) {
//			throw new IllegalStateException("상세 금액 합계와 총액이 불일치");
//		}
//	}
}
