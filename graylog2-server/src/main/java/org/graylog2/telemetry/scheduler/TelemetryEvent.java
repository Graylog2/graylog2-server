package org.graylog2.telemetry.scheduler;

import java.util.Map;

public record TelemetryEvent(Map<String, Object> metrics) {
}
