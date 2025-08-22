//package com.system.payment.integration;
//
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.spi.ILoggingEvent;
//import ch.qos.logback.core.read.ListAppender;
//import com.system.payment.payment.model.dto.InicisBillingApproval;
//import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
//import com.system.payment.payment.service.PaymentConsumer;
//import com.system.payment.payment.service.PaymentProducer;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.junit.jupiter.api.Test;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Supplier;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//
//// ... 기존 import 생략 ...
//
//@SpringBootTest
//@EmbeddedKafka(partitions = 2, topics = {PaymentProducer.PAYMENT_REQUESTED_TOPIC})
//@ActiveProfiles("test")
//class PaymentConsumerIntegrationTest {
//
//    @Autowired
//    EmbeddedKafkaBroker embeddedKafka;
//
//    @DynamicPropertySource
//    static void kafkaProps(DynamicPropertyRegistry r) {
//        r.add("spring.kafka.bootstrap-servers",
//                () -> System.getProperty("spring.embedded.kafka.brokers"));
//
//        // Producer
//        r.add("spring.kafka.producer.key-serializer",
//                () -> "org.apache.kafka.common.serialization.StringSerializer");
//        r.add("spring.kafka.producer.value-serializer",
//                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
//        r.add("spring.kafka.producer.properties.partitioner.class",
//                () -> "com.system.payment.payment.partitioner.ConsistentHashPartitioner");
//        r.add("spring.kafka.producer.properties.enable.idempotence", () -> "true");
//        r.add("spring.kafka.producer.properties.acks", () -> "all");
//
//        // Consumer (프로퍼티만)
//        r.add("spring.kafka.consumer.group-id", () -> "payment-request-consumer");
//        r.add("spring.kafka.consumer.auto-offset-reset", () -> "latest");
//        r.add("spring.kafka.consumer.key-deserializer",
//                () -> "org.apache.kafka.common.serialization.StringDeserializer");
//        r.add("spring.kafka.consumer.value-deserializer",
//                () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
//        r.add("spring.kafka.consumer.properties.spring.json.trusted.packages",
//                () -> "com.system.payment.*");
//        r.add("spring.kafka.consumer.properties.spring.json.use.type.headers",
//                () -> "false");
//        r.add("spring.kafka.consumer.properties.spring.json.value.default.type",
//                () -> "com.system.payment.payment.model.dto.PaymentRequestedMessageV1");
//    }
//
//    private KafkaTemplate<String, PaymentRequestedMessageV1<InicisBillingApproval>> kafkaTemplate() {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
//        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
//    }
//
//    @Test
//    void logs_when_message_consumed() throws Exception {
//        // 1) PaymentConsumer 로거에 ListAppender 부착
//        Logger logger = (Logger) LoggerFactory.getLogger(PaymentConsumer.class);
//        ListAppender<ILoggingEvent> appender = new ListAppender<>();
//        appender.start();
//        logger.addAppender(appender);
//
//        // 2) 테스트 메시지 준비
//        var data = InicisBillingApproval.Data.create(
//                "https://example.ngrok-free.app",
//                "DemoTest_1755064988200",
//                "AI 라이센스 키(연 1석)",
//                "JEONGYUNHWAN",
//                "test1234@naver.com",
//                "01087554034",
//                "1",
//                "billkey-xyz"
//        );
//        var approval = InicisBillingApproval.create(
//                "INIBillTst", "billing", "card", "20250820132222",
//                "10.40.212.158", "hash-abc", data
//        );
//        var envelope = PaymentRequestedMessageV1.Envelope.newEnvelope("payment-api", "idemp-001");
//        var identifiers = new PaymentRequestedMessageV1.Identifiers(1, "txn-001", 42);
//        var payment = new PaymentRequestedMessageV1.Payload.PaymentSnapshot(
//                1, "KRW", "CARD", 1, "00", Instant.now()
//        );
//        var external = new PaymentRequestedMessageV1.Payload.External<>("INICIS", approval);
//        var payload = new PaymentRequestedMessageV1.Payload<>(payment, external);
//        var event = new PaymentRequestedMessageV1<>(envelope, identifiers, payload);
//
//        // 3) 동기 전송 (브로커 write 보장)
//        var template = kafkaTemplate();
//        template.send(PaymentProducer.PAYMENT_REQUESTED_TOPIC, identifiers.partitionKey(), event).get();
//
//        // 4) 최대 5초 대기하며 로그에 "[CONSUME]"가 찍혔는지 폴링
//        boolean seen = waitUntil(() -> appender.list.stream()
//                .anyMatch(ev -> {
//                    String msg = ev.getFormattedMessage();
//                    return msg.contains("[CONSUME]")
//                            && msg.contains("txId=txn-001")
//                            && msg.contains("provider=INICIS");
//                }), 5000);
//
//        logger.detachAppender(appender); // 정리
//        assertThat(seen).isTrue();
//    }
//
//    // 유틸: 조건이 참이 될 때까지 간단 폴링 (Awaitility 없어도 사용 가능)
//    private static boolean waitUntil(Supplier<Boolean> cond, long timeoutMs) throws InterruptedException {
//        long start = System.currentTimeMillis();
//        while (System.currentTimeMillis() - start < timeoutMs) {
//            if (cond.get()) return true;
//            Thread.sleep(50);
//        }
//        return false;
//    }
//}
