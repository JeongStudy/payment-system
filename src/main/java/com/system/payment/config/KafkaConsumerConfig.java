package com.system.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    // TODO: bootstrap-servers, group-id 등은 application-{profile}.yml로 관리
    // TODO: ConsumerFactory<String, ?> 빈 정의 (Deserializer/TrustedPackages 등)
    // TODO: ConcurrentKafkaListenerContainerFactory<String, ?> 빈 정의 (concurrency, 에러핸들러, ack-mode 등)
    // TODO: CommonErrorHandler / SeekToCurrentErrorHandler / DeadLetterPublishingRecoverer 등 필요 시 추가
}