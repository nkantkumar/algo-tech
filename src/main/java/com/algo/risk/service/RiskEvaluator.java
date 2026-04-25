package com.algo.risk.service;

import com.algo.risk.dto.OrderEvent;
import com.algo.risk.dto.RiskDecision;
import reactor.core.publisher.Mono;

public interface RiskEvaluator {
    Mono<RiskDecision> evaluate(OrderEvent orderEvent);
}
