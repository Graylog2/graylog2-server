package org.graylog2.benchmarks.pipeline.singlebuffer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.WorkHandler;
import org.graylog2.benchmarks.utils.TimeCalculator;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.graylog2.benchmarks.utils.BusySleeper.consumeCpuFor;

public class FilterWorker implements WorkHandler<Event> {

    private final MetricRegistry metricRegistry;
    private final TimeCalculator timeCalculator;
    private final int ordinal;
    private final Meter processed;
    private final Timer filterTime;

    @AssistedInject
    public FilterWorker(MetricRegistry metricRegistry,
                        @Assisted TimeCalculator timeCalculator,
                        @Assisted("ordinal") int ordinal) {
        this.metricRegistry = metricRegistry;
        this.timeCalculator = timeCalculator;
        this.ordinal = ordinal;
        processed = metricRegistry.meter(metricName("processed"));
        filterTime = metricRegistry.timer(metricName("timer"));
    }

    private String metricName(String suffix) {
        return "filter-handler" + ordinal + "." + suffix;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        final Timer.Context context = filterTime.time();

        consumeCpuFor(timeCalculator.sleepTimeNsForThread(ordinal), NANOSECONDS);

        event.getRawMessage().setProcessed();
        event.setOutputMessage(new ProcessedMessage());

        processed.mark();
        context.stop();
    }

    public interface Factory {
        FilterWorker create(TimeCalculator timeCalculator,
                            @Assisted("ordinal") int ordinal);
    }
}
