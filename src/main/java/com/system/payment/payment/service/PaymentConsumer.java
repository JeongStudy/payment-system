package com.system.payment.payment.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentProcessService paymentProcessService;

    @KafkaListener(
            topics = "payment.requested.v1",
            groupId = "payment-request-consumer"
            // TODO: 필요 시 containerFactory 지정
    )
    public void onMessage(
            // TODO: 메시지 DTO 타입 명시 (예: PaymentRequestedMessageV1<?>)
            Object message,
            ConsumerRecord<String, Object> record
    ) {
        // TODO: 헤더/트레이싱/밸리데이션/로그
        // TODO: 멱등성/중복처리 가드
        // TODO: 서비스 위임
        // paymentProcessService.process(message);
    }
}

