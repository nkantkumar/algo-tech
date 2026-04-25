package com.algo.risk.repository.entity;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("trade_history")
public record TradeHistory(
        @Id Long id,
        String orderId,
        String traderId,
        String instrumentId,
        long quantity,
        BigDecimal price,
        Instant createdAt
) {
}
