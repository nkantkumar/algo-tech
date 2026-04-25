package com.algo.risk.risk;

import com.algo.risk.config.RiskProperties;
import com.algo.risk.domain.enums.Side;
import com.algo.risk.dto.OrderEvent;
import com.algo.risk.risk.check.DuplicateOrderCheck;
import com.algo.risk.service.RedisRiskStateService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DuplicateOrderCheckTest {

    @Test
    void shouldRejectDuplicateOrder() {
        RedisRiskStateService stateService = new RedisRiskStateService(null) {
            @Override
            public Mono<Boolean> registerOrderIfNew(String orderId, java.time.Duration ttl) {
                return Mono.just(false);
            }
        };
        RiskProperties properties = new RiskProperties();
        properties.setPositionLimits(Map.of("AAPL", 1000L));
        DuplicateOrderCheck check = new DuplicateOrderCheck(stateService, properties);
        OrderEvent event = new OrderEvent("o-1", "t-1", "AAPL", Side.BUY, 10, BigDecimal.TEN, Instant.now());

        StepVerifier.create(check.evaluate(event))
                .expectNextMatches(result -> !result.passed() && result.checkName().equals("duplicate_order"))
                .verifyComplete();
    }
}
