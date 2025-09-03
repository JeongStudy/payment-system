package com.system.payment.util;

import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public abstract class KafkaIntegrationTestSupport {

    protected abstract String topic();  // 예: "payment.requested.v1"
    protected String dltTopic() { return topic() + ".DLT"; }

    // 메시지 공통 상수 (필요시 protected로 제공)
    protected static final String URL = "https://billing";
    protected static final String MID = "INIBillTst";
    protected static final String GOOD_NAME = "상품명";
    protected static final String BUYER_NAME = "홍길동";
    protected static final String BUYER_EMAIL = "buyer@test.com";
    protected static final String BUYER_TEL = "010-1234-5678";
    protected static final String BILL_KEY = "BILL-KEY-123";
    protected static final String TYPE = "Billing";
    protected static final String PAY_METHOD = "card";
    protected static final String TIMESTAMP = "20250902123045";
    protected static final String CLIENT_IP = "127.0.0.1";
    protected static final String HASHED_DATA = "HASHED_DATA";
    protected static final String CURRENCY = "KRW";
    protected static final String METHOD_TYPE = "CARD";
    protected static final String PAYMENT_STATUS = "REQUEST";
    protected static final String PG_CODE = "INICIS";

    protected static final int USER_ID = 1;
    protected static final int CARD_ID = 10;
    protected static final int PAYMENT_ID = 100;
    protected static final int AMOUNT = 1000;

    // 시나리오 구분용 MOID
    protected static final String SUCCESS_MOID = "SUCCESS_MOID";
    protected static final String FAIL_MOID = "FAIL_MOID";

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    protected KafkaListenerEndpointRegistry registry;

    @Autowired
    protected EmbeddedKafkaBroker embeddedKafka;

    @BeforeEach
    void waitForAssignment() {
         /*
            @EmbeddedKafka 사용하면 테스트가 시작될 때 임베디드 Kafka 브로커가 뜸
            동시에 @kafkaListener로 등록된 리스너 컨테이너들이 브로커와 연결을 맺음

            컨테이너가 파티션 할당을 끝내기 전에 메시지를 보내버리면 메시지를 소비하지 못하고 흘려버릴 수 있음
            waitForAssignment를 사용하여 컨슈머가 모든 파티션에 완전히 붙을 때까지 블로킹 대기하는 방식으로 사용
         */
        registry.getListenerContainers().forEach(
                c -> ContainerTestUtils.waitForAssignment(c, embeddedKafka.getPartitionsPerTopic())
        );
    }

    @DynamicPropertySource
    static void dynamicKafkaProps(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers"));

        // Producer
        r.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        r.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        r.add("spring.kafka.producer.properties.spring.json.add.type.headers", () -> "true");

        // Consumer (ByteArray + JsonMessageConverter 경로)
        r.add("spring.kafka.consumer.group-id", () -> "payment-consumer-" + IdGeneratorUtil.UUIDGenerate());
        r.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        r.add("spring.kafka.consumer.key-deserializer", () -> "org.apache.kafka.common.serialization.StringDeserializer");
        r.add("spring.kafka.consumer.value-deserializer", () -> "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        // 타임아웃/하트비트
        r.add("spring.kafka.consumer.properties.heartbeat.interval.ms", () -> "3000");
        r.add("spring.kafka.consumer.properties.session.timeout.ms", () -> "30000");
        r.add("spring.kafka.consumer.properties.max.poll.interval.ms", () -> "300000");
    }

    // ------ 메시지 빌더 ------
    protected InicisBillingApproval approval(String moid) {
        return InicisBillingApproval.create(
                MID, TYPE, PAY_METHOD, TIMESTAMP, CLIENT_IP, HASHED_DATA,
                new InicisBillingApproval.Data(URL, moid, GOOD_NAME, BUYER_NAME, BUYER_EMAIL, BUYER_TEL, String.valueOf(AMOUNT), BILL_KEY)
        );
    }

    protected PaymentRequestedMessageV1.Payload<InicisBillingApproval> payload(InicisBillingApproval appr) {
        return new PaymentRequestedMessageV1.Payload<>(
                new PaymentRequestedMessageV1.Payload.PaymentSnapshot(
                        AMOUNT, CURRENCY, METHOD_TYPE, CARD_ID, PAYMENT_STATUS, Instant.now()
                ),
                new PaymentRequestedMessageV1.Payload.External<>(PG_CODE, appr)
        );
    }

    protected PaymentRequestedMessageV1<InicisBillingApproval> message(String idem, String txId, InicisBillingApproval appr) {
        return PaymentRequestedMessageV1.of(
                "payment-api",
                idem,
                new PaymentRequestedMessageV1.Identifiers(PAYMENT_ID, txId, USER_ID),
                payload(appr)
        );
    }

    // ------ 전송/폴링 헬퍼 ------
    protected void send(Object msg) {
        kafkaTemplate.send(topic(), "k", msg);
        kafkaTemplate.flush();
    }

    // 특정 토픽에서 1건이라도 읽히는지 폴링하여 확인
    protected ConsumerRecord<String, byte[]> pollOne(String topic, Duration timeout) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                "test-consumer-" + IdGeneratorUtil.UUIDGenerate(), "false", embeddedKafka);
        props.put("key.deserializer", StringDeserializer.class);
        props.put("value.deserializer", ByteArrayDeserializer.class);

        try (Consumer<String, byte[]> consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props)) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic);
            long endAt = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < endAt) {
                var records = consumer.poll(Duration.ofMillis(200));
                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
            return null;
        }
    }

    protected ConsumerRecord<String, byte[]> pollOneFromDLT(Duration timeout) {
        return pollOne(dltTopic(), timeout);
    }
}
