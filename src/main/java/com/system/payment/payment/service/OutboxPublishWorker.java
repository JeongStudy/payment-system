package com.system.payment.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.payment.domain.outbox.PaymentRequestedArgs;
import com.system.payment.payment.repository.OutboxEventRepository;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxPublishWorker {
	private final OutboxEventRepository repo;
	private final OutboxService outboxService;
	private final PaymentRepository paymentRepository;
	private final PaymentUserCardRepository cardRepo;
	private final UserService userService;
	private final PaymentProducer paymentProducer;
	private final ObjectMapper mapper;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processOne(Long eventId) throws Exception {
		var e = repo.findByIdForUpdate(eventId).orElseThrow();
		if (!"PENDING".equals(e.getStatus())) return;
		if (!"PAYMENT_REQUESTED_V1".equals(e.getEventType())) return;

		var args = mapper.readValue(e.getPayload(), PaymentRequestedArgs.class);
		var payment = paymentRepository.findById(args.paymentId()).orElseThrow();
		var user = userService.findUser();                  // 필요 시 메서드 조정
		var card = cardRepo.findById(args.methodId()).orElseThrow();

		paymentProducer.sendPaymentRequested(payment, user, card, args.productName());
		outboxService.markSent(e);
	}
}