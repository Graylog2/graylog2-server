package org.graylog2.telemetry;

import org.graylog2.plugin.PluginModule;
import org.graylog2.telemetry.scheduler.TelemetrySubmissionPeriodical;

public class TelemetryModule extends PluginModule {
    @Override
    protected void configure() {
        // Initializing binder so it can be injected with no actual bindings
        telemetryMetricSupplierBinder();
        
        addPeriodical(TelemetrySubmissionPeriodical.class);
    }
}
