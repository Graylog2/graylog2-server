package org.graylog2.inputs.diagnosis;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.graylog2.shared.metrics.MetricUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class InputDiagnosisMetrics {

    private static final int SIZE_FOR_15_MINUTES = 15;
    private final Map<String, CircularFifoQueue<Long>> metrics = new HashMap<>();
    private final AtomicReference<MetricRegistry> localMetricRegistry = new AtomicReference<>(new MetricRegistry());
    private final MetricRegistry metricRegistry;

    @Inject
    public InputDiagnosisMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void incCount(String metricName) {
        localMetricRegistry.get().counter(metricName).inc();
    }

    void update() {
        MetricRegistry registry = localMetricRegistry.getAndSet(new MetricRegistry());
        registry.getCounters().forEach((metric, counter) ->
                metrics.compute(metric, (m, q) -> {
                    final CircularFifoQueue<Long> queue = Objects.requireNonNullElseGet(q, () -> new CircularFifoQueue<>(SIZE_FOR_15_MINUTES));
                    queue.add(counter.getCount());
                    MetricUtils.safelyRegister(metricRegistry, metric, (Gauge<Long>) () -> queue.stream()
                            .mapToLong(value -> value)
                            .sum());
                    return queue;
                }));
    }

}
