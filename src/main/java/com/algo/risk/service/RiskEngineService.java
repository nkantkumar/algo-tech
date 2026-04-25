package com.algo.risk.service;

import com.algo.risk.domain.enums.RiskStatus;
import com.algo.risk.domain.enums.Side;
import com.algo.risk.dto.OrderEvent;
import com.algo.risk.dto.RiskDecision;
import com.algo.risk.messaging.FixOrderRouter;
import com.algo.risk.messaging.RiskDecisionPublisher;
import com.algo.risk.repository.entity.AuditLog;
import com.algo.risk.repository.entity.TradeHistory;
import com.algo.risk.risk.RiskCheck;
import com.algo.risk.risk.RiskCheckResult;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.algo.risk.config.RiskProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RiskEngineService implements RiskEvaluator {
    private final List<RiskCheck> checks;
    private final RiskDecisionPublisher decisionPublisher;
    private final FixOrderRouter fixOrderRouter;
    private final DownstreamPersistenceService persistenceService;
    private final RedisRiskStateService stateService;
    private final PreTradePipelineRiskService pipelineRiskService;
    private final RiskProperties riskProperties;
    private final MeterRegistry meterRegistry;

    public RiskEngineService(
            List<RiskCheck> checks,
            RiskDecisionPublisher decisionPublisher,
            FixOrderRouter fixOrderRouter,
            DownstreamPersistenceService persistenceService,
            RedisRiskStateService stateService,
            PreTradePipelineRiskService pipelineRiskService,
            RiskProperties riskProperties,
            MeterRegistry meterRegistry) {
        this.checks = checks;
        this.decisionPublisher = decisionPublisher;
        this.fixOrderRouter = fixOrderRouter;
        this.persistenceService = persistenceService;
        this.stateService = stateService;
        this.pipelineRiskService = pipelineRiskService;
        this.riskProperties = riskProperties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<RiskDecision> evaluate(OrderEvent orderEvent) {
        return pipelineRiskService.fetchSnapshot(orderEvent)
                .flatMap(snapshot -> preTradeFastReject(orderEvent, snapshot))
                .switchIfEmpty(Flux.fromIterable(checks)
                .concatMap(check -> check.evaluate(orderEvent))
                .filter(result -> !result.passed())
                .next()
                .switchIfEmpty(Mono.just(RiskCheckResult.pass("all_checks"))))
                .flatMap(result -> buildDecision(orderEvent, result))
                .flatMap(this::publishAuditAndRoute);
    }

    private Mono<RiskCheckResult> preTradeFastReject(OrderEvent event, PreTradePipelineSnapshot snapshot) {
        long maxPosition = riskProperties.getPositionLimits().getOrDefault(event.instrumentId(), 10_000L);
        if (Math.abs(snapshot.projectedPosition()) > maxPosition) {
            return Mono.just(RiskCheckResult.fail("position_limit", "position limit breached"));
        }
        if (snapshot.orderRateCount() > riskProperties.getMaxOrdersPerSecond()) {
            return Mono.just(RiskCheckResult.fail("order_rate_limit", "rate limit exceeded"));
        }
        if (snapshot.currentPnl().compareTo(riskProperties.getMaxDrawdown().negate()) < 0) {
            return Mono.just(RiskCheckResult.fail("pnl_drawdown", "max drawdown exceeded"));
        }
        if (snapshot.projectedExposure().compareTo(riskProperties.getMaxExposure()) > 0) {
            return Mono.just(RiskCheckResult.fail("exposure_limit", "total exposure breached"));
        }
        if (snapshot.duplicateOrder()) {
            return Mono.just(RiskCheckResult.fail("duplicate_order", "duplicate order detected"));
        }
        return Mono.empty();
    }

    private Mono<RiskDecision> buildDecision(OrderEvent orderEvent, RiskCheckResult result) {
        if (result.passed()) {
            meterRegistry.counter("order.accepted").increment();
            return applyAcceptedState(orderEvent)
                    .thenReturn(new RiskDecision(orderEvent.orderId(), RiskStatus.ACCEPTED, "accepted", Instant.now()));
        }
        meterRegistry.counter("order.rejected").increment();
        meterRegistry.counter("risk.breach", "check", result.checkName()).increment();
        return Mono.just(new RiskDecision(orderEvent.orderId(), RiskStatus.REJECTED, result.message(), Instant.now()));
    }

    private Mono<Void> applyAcceptedState(OrderEvent orderEvent) {
        long delta = orderEvent.side() == Side.BUY ? orderEvent.quantity() : -orderEvent.quantity();
        BigDecimal notionalDelta = orderEvent.notional();
        if (orderEvent.side() == Side.SELL) {
            notionalDelta = notionalDelta.negate();
        }
        return Mono.when(
                stateService.incrementPosition(orderEvent.instrumentId(), delta),
                stateService.incrementExposure(notionalDelta),
                stateService.incrementPnl(orderEvent.traderId(), BigDecimal.ZERO)
        ).then();
    }

    private Mono<RiskDecision> publishAuditAndRoute(RiskDecision decision) {
        Mono<Void> decisionOut = decisionPublisher.publish(decision);
        Mono<Void> audit = persistenceService.saveAudit(new AuditLog(
                null, decision.orderId(), decision.status().name(), decision.reason(), decision.decisionTime())).then();
        return Mono.when(decisionOut, audit).thenReturn(decision);
    }

    public Mono<Void> processAcceptedOrder(OrderEvent orderEvent) {
        return persistenceService.saveTrade(new TradeHistory(
                        null,
                        orderEvent.orderId(),
                        orderEvent.traderId(),
                        orderEvent.instrumentId(),
                        orderEvent.quantity(),
                        orderEvent.price(),
                        Instant.now()))
                .then(fixOrderRouter.route(orderEvent));
    }
}
