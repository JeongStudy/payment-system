package com.system.payment.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.payment.model.dto.InicisBillingApproval;
import com.system.payment.payment.model.dto.PaymentRequestedMessageV1;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    // TODO: bootstrap-servers, group-id 등은 application-{profile}.yml로 관리
    // TODO: ConsumerFactory<String, ?> 빈 정의 (Deserializer/TrustedPackages 등)
    // TODO: ConcurrentKafkaListenerContainerFactory<String, ?> 빈 정의 (concurrency, 에러핸들러, ack-mode 등)
    // TODO: CommonErrorHandler / SeekToCurrentErrorHandler / DeadLetterPublishingRecoverer 등 필요 시 추가

    private final KafkaProperties kafkaProperties;

    // 제네릭 포함 FQCN으로 값 타입을 고정 (패키지 경로는 그대로 사용)
    private static final String VALUE_DEFAULT_TYPE =
            "com.system.payment.payment.model.dto.PaymentRequestedMessageV1" +
                    "<com.system.payment.payment.model.dto.InicisBillingApproval>";

    @Bean
    public ConsumerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>> paymentConsumerFactory() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JsonDeserializer<PaymentRequestedMessageV1<InicisBillingApproval>> valueDeserializer =
                new JsonDeserializer<>(PaymentRequestedMessageV1.class, objectMapper, false);
        valueDeserializer.addTrustedPackages("com.system.payment.*");
        valueDeserializer.ignoreTypeHeaders();

        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, VALUE_DEFAULT_TYPE); // 제네릭 포함 기본 타입 고정
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean(name = "paymentKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>> paymentKafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>> paymentConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentConsumerFactory);

        // 재시도/백오프/예외정책
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5_000);

        DefaultErrorHandler kafkaErrorHandler = new DefaultErrorHandler(
                (consumerRecord, ex) -> {
                    // DeadLetterPublishingRecoverer 사용 시 구성
                },
                backOff
        );
        kafkaErrorHandler.setAckAfterHandle(true);
        kafkaErrorHandler.addNotRetryableExceptions(IllegalArgumentException.class); // 밸리데이션류는 재시도X

        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}
