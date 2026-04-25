package com.algo.risk.dto;

import com.algo.risk.domain.enums.RiskStatus;
import java.time.Instant;

public record RiskDecision(
        String orderId,
        RiskStatus status,
        String reason,
        Instant decisionTime
) {
}
