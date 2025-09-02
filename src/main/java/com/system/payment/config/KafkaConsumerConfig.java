package com.system.payment.config;

import com.system.payment.exception.PaymentDeclinedException;
import com.system.payment.exception.PaymentValidationException;
import com.system.payment.exception.TransientPgException;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.SocketTimeoutException;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<Object, Object> kafkaTemplate; // DLT 발행에 필요

    @Bean
    public ConsumerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>> paymentConsumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);

        /*
            FIXME:
            기본적으로 yml 설정이 적용되지만, 덮어씌어지는 설정
            현재는 값이 같아서 코드로 적을 필요가 없음
         */
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 10 * 60_000); // poll() 호출 사이의 최대 허용 시간
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1); // poll() 호출 시 가져올 최대 메시지 수
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15_000); // 컨슈머가 브로커에게 신호를 보내지 않고 이 시간 이상 지나면 세션 끊김
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3_000); // 컨슈머가 브로커에게 신호를 보내는 주기
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // 트랜잭션이 커밋된 메시지만 읽음

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
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
    // TODO: DLT/재시도 테스트 로직 작성 필요
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

        factory.setRecordMessageConverter(new JsonMessageConverter());

        // (선택) 수동 ACK 쓸 경우
        // factory.getContainerProperties().setAckMode(AckMode.MANUAL);

        // 에러 핸들러 세팅
        factory.setCommonErrorHandler(paymentErrorHandler);

        // (선택) DeliveryAttempt 헤더 추가
        // factory.setDeliveryAttemptHeader(true);

        return factory;
    }
}
