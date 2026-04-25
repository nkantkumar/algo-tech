package com.algo.risk.service;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RedisRiskStateService {
    private final ReactiveStringRedisTemplate redis;

    public RedisRiskStateService(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    public Mono<Long> getPosition(String instrumentId) {
        return redis.opsForValue()
                .get("pos:" + instrumentId)
                .map(Long::parseLong)
                .defaultIfEmpty(0L);
    }

    public Mono<Long> incrementPosition(String instrumentId, long delta) {
        return redis.opsForValue().increment("pos:" + instrumentId, delta);
    }

    public Mono<Long> incrementOrderRate(String traderId, String secondBucket) {
        String key = "rate:" + traderId + ":" + secondBucket;
        return redis.opsForValue().increment(key)
                .flatMap(count -> redis.expire(key, Duration.ofSeconds(2)).thenReturn(count));
    }

    public Mono<BigDecimal> totalExposure() {
        return redis.opsForValue()
                .get("exposure:total")
                .map(BigDecimal::new)
                .defaultIfEmpty(BigDecimal.ZERO);
    }

    public Mono<BigDecimal> incrementExposure(BigDecimal delta) {
        String key = "exposure:total";
        return redis.opsForValue().get(key)
                .defaultIfEmpty("0")
                .map(BigDecimal::new)
                .flatMap(current -> {
                    BigDecimal updated = current.add(delta);
                    return redis.opsForValue().set(key, updated.toPlainString()).thenReturn(updated);
                });
    }

    public Mono<BigDecimal> currentPnl(String traderId) {
        return redis.opsForValue()
                .get("pnl:" + traderId)
                .map(BigDecimal::new)
                .defaultIfEmpty(BigDecimal.ZERO);
    }

    public Mono<BigDecimal> incrementPnl(String traderId, BigDecimal delta) {
        String key = "pnl:" + traderId;
        return redis.opsForValue().get(key)
                .defaultIfEmpty("0")
                .map(BigDecimal::new)
                .flatMap(current -> {
                    BigDecimal updated = current.add(delta);
                    return redis.opsForValue().set(key, updated.toPlainString()).thenReturn(updated);
                });
    }

    public Mono<Boolean> registerOrderIfNew(String orderId, Duration ttl) {
        return redis.opsForValue()
                .setIfAbsent("dup:" + orderId, "1", ttl)
                .defaultIfEmpty(Boolean.FALSE);
    }
}
