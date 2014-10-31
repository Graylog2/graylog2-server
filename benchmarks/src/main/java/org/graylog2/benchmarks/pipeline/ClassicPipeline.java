package org.graylog2.benchmarks.pipeline;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ClassicPipeline {

    private final MessageProducer producer;

    @AssistedInject
    public ClassicPipeline(@Assisted("numFilterHandler") int numFilterHandler,
                           @Assisted("numOutputHandler") int numOutputHandler,
                           @Assisted("inputBufferSize") int inputBufferSize,
                           @Assisted("outputBufferSize") int outputBufferSize,
                           MetricRegistry metrics,
                           MessageProducer.Factory producerFactory,
                           InputBuffer.Factory inputBufferFactory,
                           OutputBuffer.Factory outputBufferFactory) {
        final OutputBuffer outputBuffer = outputBufferFactory.create(2048, numOutputHandler);
        final InputBuffer inputBuffer = inputBufferFactory.create(outputBuffer, 2048, numFilterHandler);

        producer = producerFactory.create(inputBuffer);
    }

    public ProcessedMessage produce() {
        return producer.produce();
    }

    public void stop() {

    }

    public interface Factory {
        ClassicPipeline create(@Assisted("numFilterHandler") int numFilterHandler,
                               @Assisted("numOutputHandler") int numOutputHandler,
                               @Assisted("inputBufferSize") int inputBufferSize,
                               @Assisted("outputBufferSize") int outputBufferSize);
    }
}
