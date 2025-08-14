package com.system.payment.payment.scheduler;

import com.system.payment.payment.domain.outbox.OutboxEvent;
import com.system.payment.payment.repository.OutboxEventRepository;
import com.system.payment.payment.service.OutboxPublishWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
	private final OutboxEventRepository repo;
	private final OutboxPublishWorker worker;

	@Scheduled(fixedDelay = 1000)
	public void publishBatch() {
		var batch = repo.pickPending(LocalDateTime.now(), PageRequest.of(0, 100));
		for (OutboxEvent e : batch) {
			try {
				worker.processOne(e.getId());   // ← 프록시 통해 @Transactional 진입
			} catch (Exception ex) {
				// 여기서는 삼켜도 OK. 재시도 스케줄은 worker에서 markFailed/backoff 하거나
				// 필요한 경우 여기서 호출하도록 변경
			}
		}
	}
}

