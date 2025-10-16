package org.graylog2.inputs;

import jakarta.inject.Inject;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InputsMetricsSupplier implements TelemetryMetricSupplier {
    private final InputService inputService;

    @Inject
    public InputsMetricsSupplier(InputService inputService) {
        this.inputService = inputService;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        Map<String, Object> metrics = new HashMap<>(inputService.totalCountByType());
        return Optional.of(TelemetryEvent.of(metrics));
    }
}
