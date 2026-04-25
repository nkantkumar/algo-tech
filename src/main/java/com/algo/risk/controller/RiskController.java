package com.algo.risk.controller;

import com.algo.risk.dto.OrderEvent;
import com.algo.risk.dto.RiskDecision;
import com.algo.risk.service.RiskEvaluator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
    private final RiskEvaluator riskEvaluator;

    public RiskController(RiskEvaluator riskEvaluator) {
        this.riskEvaluator = riskEvaluator;
    }

    @PostMapping("/evaluate")
    public Mono<RiskDecision> evaluate(@RequestBody OrderEvent orderEvent) {
        return riskEvaluator.evaluate(orderEvent);
    }
}
