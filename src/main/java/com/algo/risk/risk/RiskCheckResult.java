package com.algo.risk.risk;

public record RiskCheckResult(
        String checkName,
        boolean passed,
        String message
) {
    public static RiskCheckResult pass(String checkName) {
        return new RiskCheckResult(checkName, true, "passed");
    }

    public static RiskCheckResult fail(String checkName, String message) {
        return new RiskCheckResult(checkName, false, message);
    }
}
