package com.system.payment.payment.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.system.payment.card.domain.PaymentUserCard;
import com.system.payment.payment.domain.Payment;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.common.util.HashUtils;
import com.system.payment.common.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PaymentProducer {
	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Value("${payment.inicis.mid}")
	private String mid;
	@Value("${payment.inicis.api-key}")
	private String iniApiKey;
	@Value("${payment.request.topic}")
	private String PAYMENT_REQUESTED_TOPIC;
	private static final String url = "https://c5af73f84ead.ngrok-free.app";

	private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());

	private static final Logger logger = LoggerFactory.getLogger(PaymentProducer.class);

	public void sendPaymentRequested(Payment payment, PaymentUser paymentUser, PaymentUserCard paymentUserCard, String productName) {
		String key = String.valueOf(paymentUser.getId());
		String clientIp = null;
		try {
			clientIp = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}

		InicisBillingApproval inicisBillingApproval = buildInicisBillingApproval(
				payment, paymentUser, paymentUserCard,
				clientIp, productName
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
										TimeUtils.toInstant(payment.getCreatedTimestamp())
								),
								new PaymentRequestedMessageV1.Payload.External<>(
										"INICIS",
										inicisBillingApproval      // 외부 승인 원본 객체
								)
						)
				);

		final CompletableFuture<SendResult<String, Object>> send = kafkaTemplate.send(PAYMENT_REQUESTED_TOPIC, key, message);
		logger.info("");
	}

	private InicisBillingApproval buildInicisBillingApproval(
			Payment payment, PaymentUser user, PaymentUserCard card,
			String clientIp, String goodName
	) {
		String timestamp = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(TS_FMT);
		String type = "billing";
		String paymethod = "card";

		InicisBillingApproval.Data data = InicisBillingApproval.Data.create(url, card.getPgOid(), goodName,
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
