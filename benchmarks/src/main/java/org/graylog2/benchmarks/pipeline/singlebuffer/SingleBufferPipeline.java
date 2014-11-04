package org.graylog2.benchmarks.pipeline.singlebuffer;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.benchmarks.utils.TimeCalculator;

public class SingleBufferPipeline {

    private final MessageBuffer buffer;
    private final MessageProducer producer;

    @AssistedInject
    public SingleBufferPipeline(@Assisted("filterSleepCalc") TimeCalculator filterTime, @Assisted("numFilterHandler") int numFilterHandler,
                                @Assisted("outputSleepCalc") TimeCalculator outputTime, @Assisted("numOutputHandler") int numOutputHandler,
                                @Assisted("inputBufferSize") int inputBufferSize,
                                MessageBuffer.Factory bufferFactory, MessageProducer.Factory producerFactory) {
        buffer = bufferFactory.create(inputBufferSize, filterTime, numFilterHandler, outputTime, numOutputHandler);

        producer = producerFactory.create(buffer);
    }

    public void stop() {
        buffer.stop();
    }

    public ProcessedMessage produce() {
        return producer.produce();
    }

    public interface Factory {
        SingleBufferPipeline create(@Assisted("filterSleepCalc") TimeCalculator filterTime, @Assisted("numFilterHandler") int numFilterHandler,
                                    @Assisted("outputSleepCalc") TimeCalculator outputTime, @Assisted("numOutputHandler") int numOutputHandler,
                                    @Assisted("inputBufferSize") int inputBufferSize);
    }
}
