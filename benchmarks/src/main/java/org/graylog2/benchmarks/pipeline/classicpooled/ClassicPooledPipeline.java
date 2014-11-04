package org.graylog2.benchmarks.pipeline.classicpooled;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.benchmarks.utils.TimeCalculator;

public class ClassicPooledPipeline {

    private final MessageProducer producer;
    private final OutputBuffer outputBuffer;
    private final WorkerPoolInputBuffer inputBuffer;

    @AssistedInject
    public ClassicPooledPipeline(@Assisted("numFilterHandler") int numFilterHandler,
                                 @Assisted("filterSleepCalc") TimeCalculator filterTime,
                                 @Assisted("numOutputHandler") int numOutputHandler,
                                 @Assisted("outputSleepCalc") TimeCalculator outputTime,
                                 @Assisted("inputBufferSize") int inputBufferSize,
                                 @Assisted("outputBufferSize") int outputBufferSize,
                                 MetricRegistry metrics,
                                 MessageProducer.Factory producerFactory,
                                 WorkerPoolInputBuffer.Factory inputBufferFactory,
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
        ClassicPooledPipeline create(@Assisted("numFilterHandler") int numFilterHandler,
                               @Assisted("filterSleepCalc") TimeCalculator filterTime,
                               @Assisted("numOutputHandler") int numOutputHandler,
                               @Assisted("outputSleepCalc") TimeCalculator outputTime,
                               @Assisted("inputBufferSize") int inputBufferSize,
                               @Assisted("outputBufferSize") int outputBufferSize);
    }
}
