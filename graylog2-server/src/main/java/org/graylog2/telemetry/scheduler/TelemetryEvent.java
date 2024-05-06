package org.graylog2.telemetry.scheduler;

import java.util.Map;

public record TelemetryEvent(Map<String, Object> metrics) {
    public static TelemetryEvent of(Map<String, Object> metrics) {
        return new TelemetryEvent(metrics);
    }

    public static TelemetryEvent of(String key, Object value) {
        return TelemetryEvent.of(Map.of(key, value));
    }
}
