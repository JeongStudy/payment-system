package com.system.payment.payment.service;

import com.system.payment.payment.model.request.InicisBillingApproval;
import com.system.payment.user.domain.PaymentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

	public static final String PAYMENT_REQUESTED_TOPIC = "payment";

    public void sendPaymentRequested(PaymentUser paymentUser, InicisBillingApproval inicisBillingApproval) {
        kafkaTemplate.send(PAYMENT_REQUESTED_TOPIC, String.valueOf(paymentUser.getId()), inicisBillingApproval);
    }
}
