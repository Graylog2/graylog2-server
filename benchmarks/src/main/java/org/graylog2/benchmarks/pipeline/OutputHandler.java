package org.graylog2.benchmarks.pipeline;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.EventHandler;
import org.graylog2.benchmarks.utils.TimeCalculator;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.graylog2.benchmarks.utils.BusySleeper.consumeCpuFor;

public class OutputHandler implements EventHandler<Event> {

    private final MetricRegistry metricRegistry;
    private final MessageOutput messageOutput;
    private final TimeCalculator timeCalculator;
    private final int ordinal;
    private final int numHandler;
    private final Meter processed;

    @AssistedInject
    public OutputHandler(MetricRegistry metricRegistry,
                         MessageOutput messageOutput,
                         @Assisted TimeCalculator timeCalculator,
                         @Assisted("ordinal") int ordinal,
                         @Assisted("numHandler") int numHandler) {
        this.metricRegistry = metricRegistry;
        this.messageOutput = messageOutput;
        this.timeCalculator = timeCalculator;
        this.ordinal = ordinal;
        this.numHandler = numHandler;
        processed = metricRegistry.meter("output-handler" + ordinal + ".processed");
    }

    @Override
    public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
        if ((sequence % numHandler) != ordinal) {
            return;
        }
        consumeCpuFor(timeCalculator.sleepTimeNsForThread(ordinal), NANOSECONDS);

        messageOutput.write(event.message);
        processed.mark();
    }

    public interface Factory {
        OutputHandler create(TimeCalculator timeCalculator, @Assisted("ordinal") int ordinal, @Assisted("numHandler") int numHandler);
    }
}
