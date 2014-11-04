package org.graylog2.benchmarks.pipeline.classicpooled;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;

public class MessageOutput {
    private final MetricRegistry metricRegistry;
    private final Meter meter;

    @Inject
    public MessageOutput(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        meter = metricRegistry.meter("message-output");
    }

    public void write(ProcessedMessage message) {
        meter.mark();
        message.setProcessed();
    }
}
