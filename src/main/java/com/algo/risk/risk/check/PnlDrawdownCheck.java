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
public class PnlDrawdownCheck implements RiskCheck {
    private final RedisRiskStateService stateService;
    private final RiskProperties riskProperties;

    public PnlDrawdownCheck(RedisRiskStateService stateService, RiskProperties riskProperties) {
        this.stateService = stateService;
        this.riskProperties = riskProperties;
    }

    @Override
    public String name() {
        return "pnl_drawdown";
    }

    @Override
    public Mono<RiskCheckResult> evaluate(OrderEvent orderEvent) {
        return stateService.currentPnl(orderEvent.traderId())
                .map(pnl -> pnl.compareTo(riskProperties.getMaxDrawdown().negate()) >= 0
                        ? RiskCheckResult.pass(name())
                        : RiskCheckResult.fail(name(), "max drawdown exceeded"));
    }
}
