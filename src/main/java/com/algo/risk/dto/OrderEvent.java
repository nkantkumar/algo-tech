package com.algo.risk.dto;

import com.algo.risk.domain.enums.Side;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderEvent(
        String orderId,
        String traderId,
        String instrumentId,
        Side side,
        long quantity,
        BigDecimal price,
        Instant eventTime
) {
    public BigDecimal notional() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
