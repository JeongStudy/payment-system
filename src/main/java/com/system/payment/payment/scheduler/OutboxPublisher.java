package com.system.payment.payment.scheduler;

import com.system.payment.payment.repository.OutboxEventRepository;
import com.system.payment.payment.service.OutboxPublishWorker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
	private final OutboxEventRepository outboxEventRepository;
	private final OutboxPublishWorker outboxPublishWorker;

	private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

	@Scheduled(fixedDelay = 3000, initialDelay = 10000)
	public void publishBatch() {
		var now = LocalDateTime.now();
		var batch = outboxEventRepository.pickPending(now, PageRequest.of(0, 100));

		batch.parallelStream().forEach(e -> {
			try {
				outboxPublishWorker.processOne(e.getId());
			} catch (Exception ex) {
				// 예외 로깅 또는 재시도 처리
				// 여기서는 삼켜도 됨(Worker가 실패 시 markFailedAndBackoff 처리)
				// 필요 시 최소 로깅만 남겨 운영 관측
				logger.error("publishBatch failed. eventId={}", e.getId(), ex);
			}
		});
	}
}

