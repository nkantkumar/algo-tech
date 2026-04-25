package com.algo.risk.messaging;

import com.algo.risk.dto.RiskDecision;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RiskDecisionPublisher {
    private final KafkaTemplate<String, RiskDecision> kafkaTemplate;
    private final String decisionsTopic;

    public RiskDecisionPublisher(
            KafkaTemplate<String, RiskDecision> kafkaTemplate,
            @Value("${app.kafka.decisions-topic:risk-decisions}") String decisionsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.decisionsTopic = decisionsTopic;
    }

    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "publishFallback")
    public Mono<Void> publish(RiskDecision decision) {
        return Mono.fromFuture(kafkaTemplate.send(decisionsTopic, decision.orderId(), decision))
                .then();
    }

    private Mono<Void> publishFallback(RiskDecision decision, Throwable throwable) {
        return Mono.error(new IllegalStateException("Risk decision publish failed", throwable));
    }
}
