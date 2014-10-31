package org.graylog2.benchmarks.pipeline;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;

public class MessageOutput {
    private final MetricRegistry metricRegistry;

    @Inject
    public MessageOutput(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void write(ProcessedMessage message) {
        message.setProcessed();
    }
}
