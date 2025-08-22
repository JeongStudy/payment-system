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
import org.springframework.core.ResolvableType;
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

    @Bean
    public ConsumerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>> paymentConsumerFactory() {
        // 🔸 valueDeserializer 인스턴스 전달 없이, 프로퍼티만!
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(name = "paymentKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>>
    paymentKafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>> cf
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedMessageV1<InicisBillingApproval>>();
        factory.setConsumerFactory(cf);

        var backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5_000);

        var eh = new DefaultErrorHandler((cr, ex) -> {}, backOff);
        eh.setAckAfterHandle(true);
        eh.addNotRetryableExceptions(IllegalArgumentException.class);
        factory.setCommonErrorHandler(eh);
        return factory;
    }
}
