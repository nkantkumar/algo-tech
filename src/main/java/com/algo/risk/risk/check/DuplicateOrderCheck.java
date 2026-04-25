package com.algo.risk.risk.check;

import com.algo.risk.config.RiskProperties;
import com.algo.risk.dto.OrderEvent;
import com.algo.risk.risk.RiskCheck;
import com.algo.risk.risk.RiskCheckResult;
import com.algo.risk.service.RedisRiskStateService;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DuplicateOrderCheck implements RiskCheck {
    private final RedisRiskStateService stateService;
    private final RiskProperties riskProperties;

    public DuplicateOrderCheck(RedisRiskStateService stateService, RiskProperties riskProperties) {
        this.stateService = stateService;
        this.riskProperties = riskProperties;
    }

    @Override
    public String name() {
        return "duplicate_order";
    }

    @Override
    public Mono<RiskCheckResult> evaluate(OrderEvent orderEvent) {
        return stateService.registerOrderIfNew(
                        orderEvent.orderId(),
                        Duration.ofSeconds(riskProperties.getDuplicateOrderTtlSeconds()))
                .map(isNew -> Boolean.TRUE.equals(isNew)
                        ? RiskCheckResult.pass(name())
                        : RiskCheckResult.fail(name(), "duplicate order detected"));
    }
}
