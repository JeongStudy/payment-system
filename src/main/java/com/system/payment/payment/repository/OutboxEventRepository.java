package com.system.payment.payment.repository;

import com.system.payment.payment.domain.outbox.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from OutboxEvent e where e.id = :id")
	Optional<OutboxEvent> findByIdForUpdate(@Param("id") Long id);

	@Query("""
			select e from OutboxEvent e
			where e.status = 'PENDING' and e.nextAttemptAt <= :now
			order by e.id asc
			""")
	List<OutboxEvent> pickPending(@Param("now") LocalDateTime now, Pageable pageable);
}
