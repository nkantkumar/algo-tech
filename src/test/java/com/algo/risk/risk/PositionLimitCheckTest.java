package com.algo.risk.risk;

import com.algo.risk.config.RiskProperties;
import com.algo.risk.domain.enums.Side;
import com.algo.risk.dto.OrderEvent;
import com.algo.risk.risk.check.PositionLimitCheck;
import com.algo.risk.service.RedisRiskStateService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PositionLimitCheckTest {

    @Test
    void shouldRejectWhenProjectedPositionExceedsLimit() {
        RedisRiskStateService stateService = new RedisRiskStateService(null) {
            @Override
            public Mono<Long> getPosition(String instrumentId) {
                return Mono.just(980L);
            }
        };
        RiskProperties properties = new RiskProperties();
        properties.setPositionLimits(Map.of("AAPL", 1000L));
        PositionLimitCheck check = new PositionLimitCheck(stateService, properties);
        OrderEvent event = new OrderEvent("o-3", "t-2", "AAPL", Side.BUY, 50, BigDecimal.valueOf(120), Instant.now());

        StepVerifier.create(check.evaluate(event))
                .expectNextMatches(result -> !result.passed() && result.checkName().equals("position_limit"))
                .verifyComplete();
    }
}
