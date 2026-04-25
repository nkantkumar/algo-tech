package com.algo.risk.risk.check;

import com.algo.risk.config.RiskProperties;
import com.algo.risk.dto.OrderEvent;
import com.algo.risk.domain.enums.Side;
import com.algo.risk.risk.RiskCheck;
import com.algo.risk.risk.RiskCheckResult;
import com.algo.risk.service.RedisRiskStateService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PositionLimitCheck implements RiskCheck {
    private final RedisRiskStateService stateService;
    private final RiskProperties riskProperties;

    public PositionLimitCheck(RedisRiskStateService stateService, RiskProperties riskProperties) {
        this.stateService = stateService;
        this.riskProperties = riskProperties;
    }

    @Override
    public String name() {
        return "position_limit";
    }

    @Override
    public Mono<RiskCheckResult> evaluate(OrderEvent orderEvent) {
        long delta = orderEvent.side() == Side.BUY ? orderEvent.quantity() : -orderEvent.quantity();
        long maxLimit = riskProperties.getPositionLimits().getOrDefault(orderEvent.instrumentId(), 10_000L);
        return stateService.getPosition(orderEvent.instrumentId())
                .map(current -> Math.abs(current + delta) <= maxLimit
                        ? RiskCheckResult.pass(name())
                        : RiskCheckResult.fail(name(), "position limit breached"));
    }
}
