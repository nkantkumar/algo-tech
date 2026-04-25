package com.algo.risk.messaging;

import com.algo.risk.dto.OrderEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class FixOrderRouter {
    private final JmsTemplate jmsTemplate;

    public FixOrderRouter(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @CircuitBreaker(name = "ibmMqRouter", fallbackMethod = "routeFallback")
    public Mono<Void> route(OrderEvent orderEvent) {
        return Mono.fromRunnable(() -> jmsTemplate.convertAndSend("FIX.ORDER.ROUTE", orderEvent)).then();
    }

    private Mono<Void> routeFallback(OrderEvent orderEvent, Throwable throwable) {
        return Mono.error(new IllegalStateException("FIX route failed", throwable));
    }
}
