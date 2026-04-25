package com.algo.risk.risk.check;

import com.algo.risk.config.RiskProperties;
import com.algo.risk.dto.OrderEvent;
import com.algo.risk.risk.RiskCheck;
import com.algo.risk.risk.RiskCheckResult;
import com.algo.risk.service.RedisRiskStateService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ExposureCheck implements RiskCheck {
    private final RedisRiskStateService stateService;
    private final RiskProperties riskProperties;

    public ExposureCheck(RedisRiskStateService stateService, RiskProperties riskProperties) {
        this.stateService = stateService;
        this.riskProperties = riskProperties;
    }

    @Override
    public String name() {
        return "exposure_limit";
    }

    @Override
    public Mono<RiskCheckResult> evaluate(OrderEvent orderEvent) {
        BigDecimal projected = orderEvent.notional();
        return stateService.totalExposure()
                .map(exposure -> exposure.add(projected).compareTo(riskProperties.getMaxExposure()) <= 0
                        ? RiskCheckResult.pass(name())
                        : RiskCheckResult.fail(name(), "total exposure breached"));
    }
}
