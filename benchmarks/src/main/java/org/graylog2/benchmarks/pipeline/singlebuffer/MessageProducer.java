package org.graylog2.benchmarks.pipeline.singlebuffer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class MessageProducer {

    private final MessageBuffer messageBuffer;
    private final Meter produced;

    @AssistedInject
    public MessageProducer(MetricRegistry metricRegistry, @Assisted MessageBuffer messageBuffer) {
        this.messageBuffer = messageBuffer;
        produced = metricRegistry.meter("message-producer.produced");

    }

    public ProcessedMessage produce() {
        final ProcessedMessage processedMessage = new ProcessedMessage();

        messageBuffer.publish(processedMessage);
        produced.mark();
        return processedMessage;
    }

    public interface Factory {
        MessageProducer create(MessageBuffer inputBuffer);
    }
}
