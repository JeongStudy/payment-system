package com.system.payment.payment.service;

import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentProcessService paymentProcessService;

    @KafkaListener(
            topics = "payment.requested.v1",
            groupId = "payment-request-consumer",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void onMessage(
            PaymentRequestedMessageV1<InicisBillingApproval> msg,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
//          ConsumerRecord<String, Object> record
    ) {
        // 지역 변수로 안전하게 풀어냄
        PaymentRequestedMessageV1.Payload<InicisBillingApproval> payload = msg.payload();
        PaymentRequestedMessageV1.Payload.External<InicisBillingApproval> external =
                payload != null ? payload.external() : null;
        InicisBillingApproval approval = external != null ? external.approval() : null;

        log.info("[CONSUME] key={}, partition={}, offset={}, idempKey={}, txId={}, provider={}, mid={}",
                key,
                partition,
                offset,
                msg.envelope().idempotencyKey(),
                msg.identifiers().transactionId(),
                msg.payload() != null && msg.payload().external() != null
                        ? msg.payload().external().provider()
                        : null,
                approval != null ? approval.getMid() : null
        );

        // TODO: 헤더/트레이싱/밸리데이션/로그
        // TODO: 멱등성/중복처리 가드
        // TODO: 서비스 위임
        // paymentProcessService.process(message);
    }
}
