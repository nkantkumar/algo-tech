package com.algo.risk.repository;

import com.algo.risk.repository.entity.AuditLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AuditLogRepository extends ReactiveCrudRepository<AuditLog, Long> {
}
