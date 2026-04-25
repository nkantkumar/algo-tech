package com.algo.risk.integration;

import com.algo.risk.controller.RiskController;
import com.algo.risk.domain.enums.RiskStatus;
import com.algo.risk.domain.enums.Side;
import com.algo.risk.dto.OrderEvent;
import com.algo.risk.dto.RiskDecision;
import com.algo.risk.service.RiskEvaluator;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = RiskController.class)
@Import(RiskControllerIntegrationTest.TestConfig.class)
class RiskControllerIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;

    @TestConfiguration
    static class TestConfig {
        @Bean
        RiskEvaluator riskEvaluator() {
            return orderEvent -> Mono.just(new RiskDecision(
                    orderEvent.orderId(), RiskStatus.ACCEPTED, "accepted", Instant.now()));
        }
    }

    @Test
    void evaluateEndpointShouldReturnDecision() {
        OrderEvent request = new OrderEvent("o-1", "trader-1", "AAPL", Side.BUY, 20, BigDecimal.valueOf(185), Instant.now());
        webTestClient.post()
                .uri("/api/risk/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ACCEPTED")
                .jsonPath("$.orderId").isEqualTo("o-1");
    }
}
