package org.graylog2.shared.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import java.util.Map;
import java.util.stream.Collectors;

public class CustomMemoryUsageGaugeSet implements MetricSet {
    @Override
    public Map<String, Metric> getMetrics() {
        return new MemoryUsageGaugeSet().getMetrics().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().replace("'", ""), Map.Entry::getValue));
    }
}
