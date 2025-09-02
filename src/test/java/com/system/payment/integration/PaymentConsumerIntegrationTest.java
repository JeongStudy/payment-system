package com.system.payment.integration;

import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import com.system.payment.payment.service.PaymentIdempotencyGuard;
import com.system.payment.payment.service.PaymentProcessService;
import com.system.payment.util.IdGeneratorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@EmbeddedKafka(partitions = 2, topics = {"payment.requested.v1"})
@ActiveProfiles("test")
class PaymentConsumerIntegrationTest {

    // ====== 공통 상수 (메시지 공통 필드) ======
    private static final String TOPIC = "payment.requested.v1";
    private static final String URL = "https://billing";
    private static final String MID = "INIBillTst";
    private static final String GOOD_NAME = "상품명";
    private static final String BUYER_NAME = "홍길동";
    private static final String BUYER_EMAIL = "buyer@test.com";
    private static final String BUYER_TEL = "010-1234-5678";
    private static final String BILL_KEY = "BILL-KEY-123";
    private static final String TYPE = "Billing";
    private static final String PAY_METHOD = "card";
    private static final String TIMESTAMP = "20250902123045";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String HASHED_DATA = "HASHED_DATA";
    private static final String PRICE = "1000";

    private static final String CURRENCY = "KRW";
    private static final String METHOD_TYPE = "CARD";
    private static final String PAYMENT_STATUS = "REQUEST";
    private static final String PG_CODE = "INICIS";

    private static final int USER_ID = 1;
    private static final int CARD_ID = 10;
    private static final int PAYMENT_ID = 100;
    private static final int AMOUNT = 1000;

    // 시나리오 구분용 MOID
    private static final String SUCCESS_MOID = "SUCCESS_MOID";
    private static final String FAIL_MOID = "FAIL_MOID";

    @MockitoBean
    PaymentProcessService paymentProcessService;

    @MockitoBean
    PaymentIdempotencyGuard idempotencyGuard;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry registry;

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    @BeforeEach
    void setUp() {
        registry.getListenerContainers().forEach(c ->
                ContainerTestUtils.waitForAssignment(c, embeddedKafka.getPartitionsPerTopic())
        );
    }

    // ====== 헬퍼: 승인 데이터, 페이로드, 메시지, 전송 ======
    private InicisBillingApproval approval(String moid) {
        return InicisBillingApproval.create(
                MID, TYPE, PAY_METHOD, TIMESTAMP, CLIENT_IP, HASHED_DATA,
                new InicisBillingApproval.Data(
                        URL, moid, GOOD_NAME, BUYER_NAME, BUYER_EMAIL, BUYER_TEL, PRICE, BILL_KEY
                )
        );
    }

    private PaymentRequestedMessageV1.Payload<InicisBillingApproval> payload(InicisBillingApproval appr) {
        return new PaymentRequestedMessageV1.Payload<>(
                new PaymentRequestedMessageV1.Payload.PaymentSnapshot(
                        AMOUNT, CURRENCY, METHOD_TYPE, CARD_ID, PAYMENT_STATUS, Instant.now()
                ),
                new PaymentRequestedMessageV1.Payload.External<>(PG_CODE, appr)
        );
    }

    private PaymentRequestedMessageV1<InicisBillingApproval> message(String idem, String txId, InicisBillingApproval appr) {
        return PaymentRequestedMessageV1.of(
                "payment-api",
                idem,
                new PaymentRequestedMessageV1.Identifiers(PAYMENT_ID, txId, USER_ID),
                payload(appr)
        );
    }

    private void send(PaymentRequestedMessageV1<?> msg) {
        kafkaTemplate.send(TOPIC, "k", msg);
        kafkaTemplate.flush();
    }

    // ====== 테스트 ======
    @Test
    @DisplayName("정상 승인 메시지 → Consumer가 process(...)를 1회 호출한다")
    void 정상승인_end2end() {
        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();

        when(idempotencyGuard.tryAcquire(any())).thenReturn(true); // E2E 진행 허용

        send(message(idem, txId, approval(SUCCESS_MOID)));

        verify(paymentProcessService, timeout(5_000)).process(any());
        verifyNoMoreInteractions(paymentProcessService);
    }

    @Test
    @DisplayName("비즈니스 실패 메시지(moid에 FAIL 포함) → 예외 없이 process(...)가 호출된다")
    void 비즈니스실패_end2end() {
        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();

        when(idempotencyGuard.tryAcquire(any())).thenReturn(true); // E2E 진행 허용

        send(message(idem, txId, approval(FAIL_MOID)));

        verify(paymentProcessService, timeout(5_000)).process(any());
        verifyNoMoreInteractions(paymentProcessService);
    }

    @Test
    @DisplayName("idempotency 획득 실패(tryAcquire=false) → process(...)가 호출되지 않는다")
    void 멱등성키_미획득() {
        // given
        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();
        var approval = approval(SUCCESS_MOID);

        when(idempotencyGuard.tryAcquire(any())).thenReturn(false);

        // when
        send(message(idem, txId, approval));

        // then
        verify(paymentProcessService, after(1000).never()).process(any());
    }

    @Test
    @DisplayName("밸리데이션 실패(idempotencyKey 공백) → idempotencyGuard/process 미호출")
    void 밸리데이션실패_idempotencyKey() {
        // given
        var idem = "   "; // 공백으로 깨뜨리기 (validate: isBlank → true)
        var txId = "tx-" + IdGeneratorUtil.UUIDGenerate();
        var approval = approval(SUCCESS_MOID);

        when(idempotencyGuard.tryAcquire(any())).thenReturn(true); // validate에서 막혀야 함

        // when
        send(message(idem, txId, approval));

        // then: validate에서 터지므로 guard/process는 절대 호출되면 안 됨
        verify(idempotencyGuard, after(1000).never()).tryAcquire(any());
        verify(paymentProcessService, after(1000).never()).process(any());
    }

    @Test
    @DisplayName("동일 idem 중복 메시지 수신 → 첫 번째만 process 호출, 두 번째는 스킵")
    void 중복메시지_idem() {
        var idem = "idem-" + IdGeneratorUtil.UUIDGenerate();
        var tx1 = "tx-" + IdGeneratorUtil.UUIDGenerate();
        var tx2 = "tx-" + IdGeneratorUtil.UUIDGenerate();
        var approval = approval(SUCCESS_MOID);

        // tryAcquire가 순서대로 true → false 리턴하도록 설정
        when(idempotencyGuard.tryAcquire(any()))
                .thenReturn(true)   // 첫 번째 메시지: 획득 성공
                .thenReturn(false); // 두 번째 메시지: 중복으로 획득 실패

        send(message(idem, tx1, approval));
        send(message(idem, tx2, approval));

        // then
        verify(idempotencyGuard, timeout(5_000).times(2)).tryAcquire(any());

        verify(paymentProcessService, timeout(5_000).times(1)).process(any());
        verifyNoMoreInteractions(paymentProcessService);
    }

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers"));
        r.add("payment.request.topic", () -> TOPIC);

        // Producer
        r.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        r.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        r.add("spring.kafka.producer.properties.spring.json.add.type.headers", () -> "true");

        // Consumer: ByteArray + JsonMessageConverter (컨피그에 설정되어 있어야 함)
        r.add("spring.kafka.consumer.group-id", () -> "payment-consumer-" + IdGeneratorUtil.UUIDGenerate());
        r.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        r.add("spring.kafka.consumer.key-deserializer", () -> "org.apache.kafka.common.serialization.StringDeserializer");
        r.add("spring.kafka.consumer.value-deserializer", () -> "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        r.add("spring.kafka.consumer.properties.heartbeat.interval.ms", () -> "3000");
        r.add("spring.kafka.consumer.properties.session.timeout.ms", () -> "30000");
        r.add("spring.kafka.consumer.properties.max.poll.interval.ms", () -> "300000");
    }
}
