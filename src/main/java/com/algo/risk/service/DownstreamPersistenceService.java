package com.algo.risk.service;

import com.algo.risk.repository.AuditLogRepository;
import com.algo.risk.repository.TradeHistoryRepository;
import com.algo.risk.repository.entity.AuditLog;
import com.algo.risk.repository.entity.TradeHistory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DownstreamPersistenceService {
    private final AuditLogRepository auditLogRepository;
    private final TradeHistoryRepository tradeHistoryRepository;

    public DownstreamPersistenceService(
            AuditLogRepository auditLogRepository,
            TradeHistoryRepository tradeHistoryRepository) {
        this.auditLogRepository = auditLogRepository;
        this.tradeHistoryRepository = tradeHistoryRepository;
    }

    @CircuitBreaker(name = "postgresAudit", fallbackMethod = "saveAuditFallback")
    public Mono<Void> saveAudit(AuditLog auditLog) {
        return auditLogRepository.save(auditLog).then();
    }

    @CircuitBreaker(name = "postgresTrade", fallbackMethod = "saveTradeFallback")
    public Mono<Void> saveTrade(TradeHistory tradeHistory) {
        return tradeHistoryRepository.save(tradeHistory).then();
    }

    private Mono<Void> saveAuditFallback(AuditLog auditLog, Throwable throwable) {
        return Mono.error(new IllegalStateException("Audit persistence failed", throwable));
    }

    private Mono<Void> saveTradeFallback(TradeHistory tradeHistory, Throwable throwable) {
        return Mono.error(new IllegalStateException("Trade persistence failed", throwable));
    }
}
