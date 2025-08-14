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

	@Scheduled(fixedDelay = 3000)
	public void publishBatch() {
		var batch = repo.pickPending(LocalDateTime.now(), PageRequest.of(0, 100));
		batch.parallelStream().forEach(e -> {
			try {
				worker.processOne(e.getId());
			} catch (Exception ex) {
				// 예외 로깅 또는 재시도 처리
			}
		});
	}
}

