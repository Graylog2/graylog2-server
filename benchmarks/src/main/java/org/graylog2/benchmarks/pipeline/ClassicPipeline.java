package org.graylog2.benchmarks.pipeline;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.benchmarks.utils.TimeCalculator;

public class ClassicPipeline {

    private final MessageProducer producer;
    private final OutputBuffer outputBuffer;
    private final InputBuffer inputBuffer;

    @AssistedInject
    public ClassicPipeline(@Assisted("numFilterHandler") int numFilterHandler,
                           @Assisted("filterSleepCalc") TimeCalculator filterTime,
                           @Assisted("numOutputHandler") int numOutputHandler,
                           @Assisted("outputSleepCalc") TimeCalculator outputTime,
                           @Assisted("inputBufferSize") int inputBufferSize,
                           @Assisted("outputBufferSize") int outputBufferSize,
                           MetricRegistry metrics,
                           MessageProducer.Factory producerFactory,
                           InputBuffer.Factory inputBufferFactory,
                           OutputBuffer.Factory outputBufferFactory) {
        outputBuffer = outputBufferFactory.create(outputTime, outputBufferSize, numOutputHandler);
        inputBuffer = inputBufferFactory.create(filterTime, outputBuffer, inputBufferSize, numFilterHandler);

        producer = producerFactory.create(inputBuffer);
    }

    public ProcessedMessage produce() {
        return producer.produce();
    }

    public void stop() {
        inputBuffer.stop();
        outputBuffer.stop();
    }

    public interface Factory {
        ClassicPipeline create(@Assisted("numFilterHandler") int numFilterHandler,
                               @Assisted("filterSleepCalc") TimeCalculator filterTime,
                               @Assisted("numOutputHandler") int numOutputHandler,
                               @Assisted("outputSleepCalc") TimeCalculator outputTime,
                               @Assisted("inputBufferSize") int inputBufferSize,
                               @Assisted("outputBufferSize") int outputBufferSize);
    }
}
