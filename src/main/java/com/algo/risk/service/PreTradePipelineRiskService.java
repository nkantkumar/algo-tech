package com.algo.risk.service;

import com.algo.risk.config.RiskProperties;
import com.algo.risk.domain.enums.Side;
import com.algo.risk.dto.OrderEvent;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class PreTradePipelineRiskService {
    private final StringRedisTemplate redisTemplate;
    private final RiskProperties riskProperties;
    private final MeterRegistry meterRegistry;

    public PreTradePipelineRiskService(
            StringRedisTemplate redisTemplate,
            RiskProperties riskProperties,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.riskProperties = riskProperties;
        this.meterRegistry = meterRegistry;
    }

    public Mono<PreTradePipelineSnapshot> fetchSnapshot(OrderEvent event) {
        return Mono.fromCallable(() -> runPipelinedFetch(event))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @SuppressWarnings("unchecked")
    private PreTradePipelineSnapshot runPipelinedFetch(OrderEvent event) {
        Instant start = Instant.now();
        String positionKey = "pos:" + event.instrumentId();
        String exposureKey = "exposure:total";
        String pnlKey = "pnl:" + event.traderId();
        String secondBucket = String.valueOf(Instant.now().getEpochSecond());
        String rateKey = "rate:" + event.traderId() + ":" + secondBucket;
        String duplicateKey = "dup:" + event.orderId();

        List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                operations.opsForValue().get((K) positionKey);
                operations.opsForValue().get((K) exposureKey);
                operations.opsForValue().get((K) pnlKey);
                operations.opsForValue().increment((K) rateKey);
                operations.expire((K) rateKey, Duration.ofSeconds(2));
                operations.opsForValue().setIfAbsent(
                        (K) duplicateKey,
                        (V) "1",
                        Duration.ofSeconds(riskProperties.getDuplicateOrderTtlSeconds()));
                return null;
            }
        });

        long currentPosition = parseLong(results.get(0), 0L);
        BigDecimal currentExposure = parseDecimal(results.get(1), BigDecimal.ZERO);
        BigDecimal pnl = parseDecimal(results.get(2), BigDecimal.ZERO);
        long rate = parseLong(results.get(3), 0L);
        boolean isNew = results.get(5) instanceof Boolean b && b;

        long delta = event.side() == Side.BUY ? event.quantity() : -event.quantity();
        BigDecimal exposureDelta = event.side() == Side.BUY ? event.notional() : event.notional().negate();
        long projectedPosition = currentPosition + delta;
        BigDecimal projectedExposure = currentExposure.add(exposureDelta);

        long elapsedNanos = Duration.between(start, Instant.now()).toNanos();
        meterRegistry.timer("risk.pretrade.pipeline.latency").record(elapsedNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        return new PreTradePipelineSnapshot(projectedPosition, projectedExposure, pnl, rate, !isNew);
    }

    private long parseLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Long.parseLong(value.toString());
    }

    private BigDecimal parseDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return new BigDecimal(value.toString());
    }
}
