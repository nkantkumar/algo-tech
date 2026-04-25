package com.algo.risk.messaging;

import com.algo.risk.domain.enums.RiskStatus;
import com.algo.risk.dto.OrderEvent;
import com.algo.risk.service.RiskEngineService;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private final RiskEngineService riskEngineService;
    private final Tracer tracer;

    public OrderEventConsumer(RiskEngineService riskEngineService, Tracer tracer) {
        this.riskEngineService = riskEngineService;
        this.tracer = tracer;
    }

    @KafkaListener(topics = "${app.kafka.orders-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderEvent event) {
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "no-trace";
        riskEngineService.evaluate(event)
                .flatMap(decision -> {
                    log.info("risk_decision traceId={} orderId={} status={} reason={}",
                            traceId, decision.orderId(), decision.status(), decision.reason());
                    if (decision.status() == RiskStatus.ACCEPTED) {
                        return riskEngineService.processAcceptedOrder(event);
                    }
                    return reactor.core.publisher.Mono.empty();
                })
                .doOnError(ex -> log.error("risk_evaluation_failed traceId={} orderId={} error={}",
                        traceId, event.orderId(), ex.getMessage(), ex))
                .subscribe();
    }
}
