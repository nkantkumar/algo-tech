package com.algo.risk.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "risk")
public class RiskProperties {
    private Map<String, Long> positionLimits = new HashMap<>();
    private int maxOrdersPerSecond = 50;
    private BigDecimal maxDrawdown = BigDecimal.valueOf(100000);
    private BigDecimal maxExposure = BigDecimal.valueOf(1_000_000);
    private int duplicateOrderTtlSeconds = 60;

    public Map<String, Long> getPositionLimits() {
        return positionLimits;
    }

    public void setPositionLimits(Map<String, Long> positionLimits) {
        this.positionLimits = positionLimits;
    }

    public int getMaxOrdersPerSecond() {
        return maxOrdersPerSecond;
    }

    public void setMaxOrdersPerSecond(int maxOrdersPerSecond) {
        this.maxOrdersPerSecond = maxOrdersPerSecond;
    }

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(BigDecimal maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public BigDecimal getMaxExposure() {
        return maxExposure;
    }

    public void setMaxExposure(BigDecimal maxExposure) {
        this.maxExposure = maxExposure;
    }

    public int getDuplicateOrderTtlSeconds() {
        return duplicateOrderTtlSeconds;
    }

    public void setDuplicateOrderTtlSeconds(int duplicateOrderTtlSeconds) {
        this.duplicateOrderTtlSeconds = duplicateOrderTtlSeconds;
    }
}
