package org.graylog2.benchmarks.pipeline;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.EventHandler;

public class FilterHandler implements EventHandler<Event> {

    private final MetricRegistry metricRegistry;
    private final OutputBuffer outputBuffer;
    private final int ordinal;
    private final int numHandler;
    private final Meter processed;

    @AssistedInject
    public FilterHandler(MetricRegistry metricRegistry,
                         @Assisted OutputBuffer outputBuffer,
                         @Assisted("ordinal") int ordinal,
                         @Assisted("numHandler") int numHandler) {
        this.metricRegistry = metricRegistry;
        this.outputBuffer = outputBuffer;
        this.ordinal = ordinal;
        this.numHandler = numHandler;
        processed = metricRegistry.meter("filter-handler" + ordinal + ".processed");
    }

    @Override
    public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
        if ((sequence % numHandler) != ordinal) {
            return;
        }
        outputBuffer.publish(event.message);
        processed.mark();
    }

    public interface Factory {
        FilterHandler create(@Assisted OutputBuffer outputBuffer, @Assisted("ordinal") int ordinal, @Assisted("numHandler") int numHandler);
    }
}
