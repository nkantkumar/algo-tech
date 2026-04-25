package com.algo.risk.risk.check;

import com.algo.risk.config.RiskProperties;
import com.algo.risk.dto.OrderEvent;
import com.algo.risk.risk.RiskCheck;
import com.algo.risk.risk.RiskCheckResult;
import com.algo.risk.service.RedisRiskStateService;
import java.time.Instant;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OrderRateLimiterCheck implements RiskCheck {
    private final RedisRiskStateService stateService;
    private final RiskProperties riskProperties;

    public OrderRateLimiterCheck(RedisRiskStateService stateService, RiskProperties riskProperties) {
        this.stateService = stateService;
        this.riskProperties = riskProperties;
    }

    @Override
    public String name() {
        return "order_rate_limit";
    }

    @Override
    public Mono<RiskCheckResult> evaluate(OrderEvent orderEvent) {
        String secondBucket = String.valueOf(Instant.now().getEpochSecond());
        return stateService.incrementOrderRate(orderEvent.traderId(), secondBucket)
                .map(count -> count <= riskProperties.getMaxOrdersPerSecond()
                        ? RiskCheckResult.pass(name())
                        : RiskCheckResult.fail(name(), "rate limit exceeded"));
    }
}
