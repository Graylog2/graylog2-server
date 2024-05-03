package org.graylog2.telemetry.scheduler;

import java.util.Optional;
import java.util.function.Supplier;

public interface TelemetryMetricSupplier extends Supplier<Optional<TelemetryEvent>> {
}
