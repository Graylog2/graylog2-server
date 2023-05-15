package org.graylog.testing.completebackend;

import java.util.Optional;

public class TimeoutHelpers {
    private static final double DEFAULT_MULTIPLIER = 1.5;

    public static Number applyTimeoutMultiplier(Number timeout) {
        return timeout.doubleValue() * timeoutMultiplier();
    }

    private static double timeoutMultiplier() {
        return Optional.ofNullable(System.getenv("TIMEOUT_MULTIPLIER"))
                .map(timeoutMultiplier -> {
                    try {
                        return Double.parseDouble(timeoutMultiplier);
                    } catch (NumberFormatException e) {
                        return DEFAULT_MULTIPLIER;
                    }
                }).orElse(DEFAULT_MULTIPLIER);
    }
}
