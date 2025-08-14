package com.system.payment.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.payment.domain.Payment;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import com.system.payment.payment.model.request.CreatePaymentRequest;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.util.HashUtils;
import com.system.payment.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.system.payment.util.TimeUtil.toInstant;

@Service
@RequiredArgsConstructor
public class PaymentProducer {
	private final KafkaTemplate<String, Object> kafkaTemplate;

	private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
	//mid 예시
	private static final String mid = "INIBillTst";
	private static final String iniApiKey = "q9G7rX1pT5vL2cN8";
	private static final String url = "https://c5af73f84ead.ngrok-free.app";

	public static final String PAYMENT_REQUESTED_TOPIC = "payment.requested.v1";

	public void sendPaymentRequested(Payment payment, PaymentUser paymentUser, PaymentUserCard paymentUserCard, String productName) {

		// TODO 유저별 파티셔닝, 같은 유저는 같은 파티션으로 가야함, 현재 서버는 2개인데, 서버가 증설될것도 고려해야함
		// 파티셔닝하는 인터페이스를 만들어서, 파티션을 만들어보자
		// 여기서의 key는 뭘까? 파티셔닝 키인지? 데이터를 구별하는 키인지?

		Integer partition = 1;
		String key = String.valueOf(paymentUser.getId());
		String clientIp = null;
		try {
			clientIp = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}

		InicisBillingApproval inicisBillingApproval = buildInicisBillingApproval(
				payment, paymentUser, paymentUserCard,
				clientIp, mid, iniApiKey, url, productName
		);

		PaymentRequestedMessageV1<InicisBillingApproval> message =
				PaymentRequestedMessageV1.of(
						"payment-api",                       // producer
						payment.getIdempotencyKey(),                      // 요청 멱등키
						new PaymentRequestedMessageV1.Identifiers(payment.getId(), payment.getTransactionId(), payment.getUserRef().getUserId()),
						new PaymentRequestedMessageV1.Payload<>(
								new PaymentRequestedMessageV1.Payload.PaymentSnapshot(
										payment.getTotalAmount(),
										"KRW",
										payment.getMethodRef().getPaymentMethodType().name(),
										paymentUserCard.getId(),
										payment.getPaymentResultCode().getCode(),
										TimeUtil.toInstant(payment.getCreatedTimestamp())
								),
								new PaymentRequestedMessageV1.Payload.External<>(
										"INICIS",
										inicisBillingApproval      // 외부 승인 원본 객체
								)
						)
				);

		kafkaTemplate.send(PAYMENT_REQUESTED_TOPIC, partition, key, message);
	}

	private InicisBillingApproval buildInicisBillingApproval(
			Payment payment, PaymentUser user, PaymentUserCard card,
			String clientIp, String mid, String iniApiKey, String url, String goodName
	) {
		String timestamp = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(TS_FMT);
		String type = "billing";
		String paymethod = "card";

		InicisBillingApproval.Data data = InicisBillingApproval.Data.create(url, card.getOid(), goodName,
				new StringBuilder().append(user.getLastName()).append(user.getFirstName()).toString(),
				user.getEmail(), user.getPhoneNumber(), String.valueOf(payment.getTotalAmount()), card.getBillingKey());

		String dataJson = toJson(data);
		String hash = HashUtils.sha512(iniApiKey + mid + type + timestamp + dataJson);

		InicisBillingApproval inicisBillingApproval = InicisBillingApproval
				.create(
						mid,
						type,
						paymethod,
						timestamp,
						clientIp,
						hash,
						data
				);

		return inicisBillingApproval;
	}

	private static String toJson(Object o) {
		try {
			return JSON.writeValueAsString(o);
		} catch (Exception e) {
			throw new IllegalStateException("data json serialize failed", e);
		}
	}
}
