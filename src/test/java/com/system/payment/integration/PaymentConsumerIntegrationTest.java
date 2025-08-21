package com.system.payment.integration;

import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import com.system.payment.payment.service.PaymentConsumer;
import com.system.payment.payment.service.PaymentProcessService;
import com.system.payment.payment.service.PaymentProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 2, topics = {PaymentProducer.PAYMENT_REQUESTED_TOPIC})
@Import(PaymentConsumerTestConfig.class) // ✅ KafkaTestConsumerConfig 제거
@ActiveProfiles("test")
class PaymentConsumerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private PaymentConsumer consumer;

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry r) {
        // EmbeddedKafka broker 연결
        r.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));

        // Producer
        r.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        r.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        r.add("spring.kafka.producer.properties.partitioner.class",
                () -> "com.system.payment.payment.partitioner.ConsistentHashPartitioner");
        r.add("spring.kafka.producer.properties.enable.idempotence", () -> "true");
        r.add("spring.kafka.producer.properties.acks", () -> "all");
        r.add("spring.kafka.producer.properties.retries", () -> "3");
        r.add("spring.kafka.producer.properties.max.in.flight.requests.per.connection", () -> "5");

        // Consumer → property 방식으로만 유지
        r.add("spring.kafka.consumer.group-id", () -> "payment-consumer");
        r.add("spring.kafka.consumer.auto-offset-reset", () -> "latest");
        r.add("spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");

        r.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
        r.add("spring.kafka.consumer.properties.spring.json.use.type.headers", () -> "false");
        r.add("spring.kafka.consumer.properties.spring.json.value.default.type",
                () -> "com.system.payment.payment.model.dto.PaymentRequestedMessageV1");

        // 세션/폴링 관련 옵션
        r.add("spring.kafka.consumer.properties.heartbeat.interval.ms", () -> "3000");
        r.add("spring.kafka.consumer.properties.session.timeout.ms", () -> "30000");
        r.add("spring.kafka.consumer.properties.max.poll.interval.ms", () -> "300000");
    }

    private KafkaTemplate<String, PaymentRequestedMessageV1<InicisBillingApproval>> kafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    void consumer_should_receive_message_and_invoke_onMessage() {
        // given
        InicisBillingApproval.Data data = InicisBillingApproval.Data.create(
                "https://example.ngrok-free.app",
                "DemoTest_1755064988200",
                "AI 라이센스 키(연 1석)",
                "JEONGYUNHWAN",
                "test1234@naver.com",
                "01087554034",
                "1",
                "billkey-xyz"
        );
        InicisBillingApproval approval = InicisBillingApproval.create(
                "INIBillTst",
                "billing",
                "card",
                "20250820132222",
                "10.40.212.158",
                "hash-abc",
                data
        );

        PaymentRequestedMessageV1.Envelope envelope =
                PaymentRequestedMessageV1.Envelope.newEnvelope("payment-api", "idemp-001");

        PaymentRequestedMessageV1.Identifiers identifiers =
                new PaymentRequestedMessageV1.Identifiers(1, "txn-001", 42);

        PaymentRequestedMessageV1.Payload.PaymentSnapshot payment =
                new PaymentRequestedMessageV1.Payload.PaymentSnapshot(
                        1, "KRW", "CARD", 1, "00", Instant.now()
                );

        PaymentRequestedMessageV1.Payload.External<InicisBillingApproval> external =
                new PaymentRequestedMessageV1.Payload.External<>("INICIS", approval);

        PaymentRequestedMessageV1.Payload<InicisBillingApproval> payload =
                new PaymentRequestedMessageV1.Payload<>(payment, external);

        PaymentRequestedMessageV1<InicisBillingApproval> event =
                new PaymentRequestedMessageV1<>(envelope, identifiers, payload);

        KafkaTemplate<String, PaymentRequestedMessageV1<InicisBillingApproval>> template = kafkaTemplate();

        // when
        template.send(PaymentRequestedMessageV1.TOPIC, identifiers.partitionKey(), event);

        // then
        verify(consumer, timeout(5000))
                .onMessage(
                        Mockito.<PaymentRequestedMessageV1<InicisBillingApproval>>any(),
                        anyString(),
                        anyInt(),
                        anyLong()
                );
    }
}

@TestConfiguration
class PaymentConsumerTestConfig {

    @Bean
    @Primary
    PaymentProcessService paymentProcessService() {
        return Mockito.mock(PaymentProcessService.class);
    }

    @Bean
    @Primary
    PaymentConsumer paymentConsumer(PaymentProcessService paymentProcessService) {
        return Mockito.spy(new PaymentConsumer(paymentProcessService));
    }
}
