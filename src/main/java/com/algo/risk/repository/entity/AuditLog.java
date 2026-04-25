package com.algo.risk.repository.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("audit_log")
public record AuditLog(
        @Id Long id,
        String orderId,
        String status,
        String reason,
        Instant createdAt
) {
}
