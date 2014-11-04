package org.graylog2.benchmarks.pipeline.classic;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class MessageProducer {

    private final EventHandlerInputBuffer inputBuffer;
    private final Meter produced;

    @AssistedInject
    public MessageProducer(MetricRegistry metricRegistry, @Assisted EventHandlerInputBuffer inputBuffer) {
        this.inputBuffer = inputBuffer;
        produced = metricRegistry.meter("message-producer.produced");

    }

    public ProcessedMessage produce() {
        final ProcessedMessage processedMessage = new ProcessedMessage();
        inputBuffer.publish(processedMessage);
        produced.mark();
        return processedMessage;
    }

    public interface Factory {
        MessageProducer create(EventHandlerInputBuffer inputBuffer);
    }
}
