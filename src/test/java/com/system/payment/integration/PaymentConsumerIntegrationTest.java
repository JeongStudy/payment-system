package com.system.payment.integration;

import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import com.system.payment.payment.service.PaymentConsumer;
import com.system.payment.payment.service.PaymentIdempotencyGuard;
import com.system.payment.payment.service.PaymentProcessService;
import com.system.payment.util.IdGeneratorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;


import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@EmbeddedKafka(partitions = 2, topics = {"payment.requested.v1"})
@ActiveProfiles("test")
class PaymentConsumerIntegrationTest {

    private static final String TOPIC = "payment.requested.v1";
    private static final String URL = "https://billing";
    private static final String MID = "INIBillTst";
    private static final String GOOD_NAME = "상품명";
    private static final String BUYER_NAME = "홍길동";
    private static final String BUYER_EMAIL = "buyer@test.com";
    private static final String BUYER_TEL = "010-1234-5678";
    private static final String BILL_KEY = "BILL-KEY-123";
    private static final String TYPE = "Billing";
    private static final String PAYMETHOD = "Card";
    private static final String TIMESTAMP = "20250902123045";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String HASHED_DATA = "HASHED_DATA";

    @MockitoBean
    PaymentProcessService paymentProcessService;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry registry;
    @Autowired
    EmbeddedKafkaBroker embeddedKafka;


    @BeforeEach
    void waitForAssignment() {
        registry.getListenerContainers().forEach(c ->
                ContainerTestUtils.waitForAssignment(c, embeddedKafka.getPartitionsPerTopic())
        );
    }

    @MockitoBean
    PaymentIdempotencyGuard idempotencyGuard;

    @Autowired
    PaymentConsumer consumer;

    @Test
    void consumerServiceCheck() {
        var serviceInConsumer = ReflectionTestUtils.getField(consumer, "paymentProcessService");
        System.out.println("👀 Consumer 내부 주입된 클래스: " + serviceInConsumer.getClass());
    }

    @Test
    @DisplayName("정상 승인 메시지 → Consumer가 process(...)를 1회 호출한다")
    void 정상승인_end2end() {
        // given
        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();

        var approval = InicisBillingApproval.create(
                "mid-test",
                "billing",
                "card",
                "20250902153000",
                "127.0.0.1",
                "hash-value",
                new InicisBillingApproval.Data(
                        "https://test-url",
                        "pg-oid",
                        "상품명",
                        "홍길동",
                        "test@email.com",
                        "01012345678",
                        "1000",
                        "billing-key-abc"
                )
        );

        var message = PaymentRequestedMessageV1.of(
                "payment-api",
                idem,
                new PaymentRequestedMessageV1.Identifiers(1001, txId, 42),
                new PaymentRequestedMessageV1.Payload<>(
                        new PaymentRequestedMessageV1.Payload.PaymentSnapshot(
                                1000,
                                "KRW",
                                "CARD",
                                100,
                                "REQUEST",
                                Instant.now()
                        ),
                        new PaymentRequestedMessageV1.Payload.External<>(
                                "INICIS",
                                approval
                        )
                )
        );

        // when
        kafkaTemplate.send(TOPIC, "k", message);
        kafkaTemplate.flush();

        // then
        verify(paymentProcessService, timeout(5_000)).process(any());
        verifyNoMoreInteractions(paymentProcessService);
    }

//    @Test
//    @DisplayName("비즈니스 실패 메시지(moid에 FAIL 포함) → 예외 없이 process(...)가 호출된다")
//    void 비즈니스실패_end2end() {
//        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
//        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();
//
//        var payload = msg(
//                "payment-api",
//                idem,
//                1001,
//                txId, 42,
//                "INICIS",
//                approval("ANY_FAIL", "1000")
//        );
//
//        Message<PaymentRequestedMessageV1<InicisBillingApproval>> message =
//                MessageBuilder.withPayload(payload)
//                        .setHeader(KafkaHeaders.TOPIC, TOPIC)
//                        .setHeader(KafkaHeaders.KEY, "k")
//                        .setHeader("X-Trace-Id", UUID.randomUUID().toString())
//                        .build();
//
//        kafkaTemplate.send(message);
//        kafkaTemplate.flush();
//
//        // 비즈니스 실패도 서비스까지 위임은 이뤄지고, 내부에서 실패 확정 처리됨
//        verify(paymentProcessService, timeout(5_000)).process(any());
//        verifyNoMoreInteractions(paymentProcessService);
//    }

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry r) {
        // 공통
        r.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));

        r.add("payment.request.topic", () -> "payment.requested.v1");

        // Producer
        r.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        r.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        r.add("spring.kafka.producer.properties.spring.json.add.type.headers", () -> "true");

        // yml의 producer.properties.* 매핑
        r.add("spring.kafka.producer.properties.partitioner.class",
                () -> "com.system.payment.payment.partitioner.ConsistentHashPartitioner");
        r.add("spring.kafka.producer.properties.enable.idempotence", () -> "true");
        r.add("spring.kafka.producer.properties.acks", () -> "all");
        r.add("spring.kafka.producer.properties.retries", () -> "3");
        r.add("spring.kafka.producer.properties.max.in.flight.requests.per.connection", () -> "5");

        // Consumer
        r.add("spring.kafka.consumer.group-id", () -> "payment-consumer-" + IdGeneratorUtil.UUIDGenerate());
        r.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        r.add("spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        r.add("spring.kafka.consumer.value-deserializer",
                () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        r.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");

        r.add("spring.kafka.consumer.properties.spring.json.use.type.headers", () -> "true");

        // yml의 consumer 타임아웃/폴링 관련은 Boot에서 properties.* 로 넘기는게 안전
        r.add("spring.kafka.consumer.properties.heartbeat.interval.ms", () -> "3000");
        r.add("spring.kafka.consumer.properties.session.timeout.ms", () -> "30000");
        r.add("spring.kafka.consumer.properties.max.poll.interval.ms", () -> "300000");
    }
}
