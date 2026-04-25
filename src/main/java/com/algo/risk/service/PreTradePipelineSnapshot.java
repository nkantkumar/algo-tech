package com.algo.risk.service;

import java.math.BigDecimal;

public record PreTradePipelineSnapshot(
        long projectedPosition,
        BigDecimal projectedExposure,
        BigDecimal currentPnl,
        long orderRateCount,
        boolean duplicateOrder
) {
}
