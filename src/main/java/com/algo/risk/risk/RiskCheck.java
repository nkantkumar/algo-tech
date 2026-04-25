package com.algo.risk.risk;

import com.algo.risk.dto.OrderEvent;
import reactor.core.publisher.Mono;

public interface RiskCheck {
    String name();

    Mono<RiskCheckResult> evaluate(OrderEvent orderEvent);
}
