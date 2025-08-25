package com.system.payment.config;

import com.system.payment.exception.PaymentDeclinedException;
import com.system.payment.exception.PaymentValidationException;
import com.system.payment.exception.TransientPgException;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.SocketTimeoutException;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    // TODO: bootstrap-servers, group-id 등은 application-{profile}.yml로 관리
    // TODO: ConsumerFactory<String, ?> 빈 정의 (Deserializer/TrustedPackages 등)
    // TODO: ConcurrentKafkaListenerContainerFactory<String, ?> 빈 정의 (concurrency, 에러핸들러, ack-mode 등)
    // TODO: CommonErrorHandler / SeekToCurrentErrorHandler / DeadLetterPublishingRecoverer 등 필요 시 추가

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<Object, Object> kafkaTemplate; // DLT 발행에 필요

    @Bean
    public ConsumerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>> paymentConsumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);

        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 10 * 60_000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15_000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3_000);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /** DLT 발행자: 원본 파티션 → 동일 파티션의 DLT로 라우팅 */
    @Bean
    public DeadLetterPublishingRecoverer paymentDltRecoverer() {
        return new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (r, e) -> {
                    // topic.payment.requested.v1 -> topic.payment.requested.v1.DLT
                    String dltTopic = r.topic() + ".DLT";
                    return new org.apache.kafka.common.TopicPartition(dltTopic, r.partition());
                });
    }

    /** 재시도/비재시도 구분 + 백오프 + DLT */
    @Bean
    public DefaultErrorHandler paymentErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        var backOff = new ExponentialBackOffWithMaxRetries(5); // 총 5회
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5_000);

        var errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 비재시도(업무) 예외
        errorHandler.addNotRetryableExceptions(
                PaymentValidationException.class,
                PaymentDeclinedException.class,
                IllegalArgumentException.class
        );

        // 역직렬화 실패는 즉시 DLT (ErrorHandlingDeserializer 사용 시)
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        // 일시적 오류(네트워크/PG 5xx 등)는 재시도 허용
        errorHandler.addRetryableExceptions(
                TransientPgException.class,
                SocketTimeoutException.class,
                HttpServerErrorException.class
        );

        errorHandler.setCommitRecovered(true); // DLT 발행 후 오프셋 커밋
        errorHandler.setAckAfterHandle(true);  // 처리 후 ack

        return errorHandler;
    }

    @Bean(name = "paymentKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>>
    paymentKafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>> cf,
            DefaultErrorHandler paymentErrorHandler
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>>();
        factory.setConsumerFactory(cf);

        // 순서 보장이 중요 → 과도한 동시성 금지
        factory.setConcurrency(3);

        // (선택) 수동 ACK 쓸 경우
        // factory.getContainerProperties().setAckMode(AckMode.MANUAL);

        // 에러 핸들러 세팅
        factory.setCommonErrorHandler(paymentErrorHandler);

        // (선택) DeliveryAttempt 헤더 추가
        // factory.setDeliveryAttemptHeader(true);

        return factory;
    }
}
