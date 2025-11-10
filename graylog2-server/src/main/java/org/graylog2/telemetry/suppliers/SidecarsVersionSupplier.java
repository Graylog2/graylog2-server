package org.graylog2.telemetry.suppliers;

import jakarta.inject.Inject;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SidecarsVersionSupplier implements TelemetryMetricSupplier {
    private final SidecarService sidecarService;

    @Inject
    public SidecarsVersionSupplier(SidecarService sidecarService) {
        this.sidecarService = sidecarService;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        Map<String, Object> metrics = new HashMap<>(sidecarService.countByVersion());

        return Optional.of(TelemetryEvent.of(metrics));
    }
}
