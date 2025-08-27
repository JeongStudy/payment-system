//package com.system.payment;
//
//import com.system.payment.payment.repository.OutboxEventRepository;
//import com.system.payment.payment.scheduler.OutboxPublishWorker;
//import com.system.payment.payment.service.InicisPgClientService;
//import com.system.payment.payment.service.PaymentProcessService;
//import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//public class CommonControllerTest {
//
//	@MockitoBean
//	public InicisPgClientService inicisPgClientService;
//
//	@MockitoBean
//	public PaymentProcessService paymentProcessService;
//
//	@MockitoBean
//	public OutboxEventRepository outboxEventRepository;
//
//	@MockitoBean
//	public OutboxPublishWorker outboxPublishWorker;
//
//	@MockitoBean
//	public KafkaListenerEndpointRegistry registry;
//
//	@MockitoBean
//	public EmbeddedKafkaBroker embeddedKafka;
//
//
//}
