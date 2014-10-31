package org.graylog2.benchmarks.pipeline;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.benchmarks.utils.TimeCalculator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OutputBuffer {

    private final RingBuffer<Event> ringBuffer;

    @AssistedInject
    public OutputBuffer(MetricRegistry metricRegistry,
                        OutputHandler.Factory handlerFactory,
                        @Assisted("filterSleepCalc") TimeCalculator outputTime,
                        @Assisted("bufferSize") int bufferSize,
                        @Assisted("numOutputHandler") int numOutputHandler) {
        final ExecutorService executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("outputbuffer-%d")
                        .build()
        );

        final Disruptor<Event> disruptor = new Disruptor<>(
                Event.FACTORY,
                bufferSize,
                executor,
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );

        final OutputHandler[] handlers = new OutputHandler[numOutputHandler];
        for (int i = 0; i < numOutputHandler; i++) {
            handlers[i] = handlerFactory.create(outputTime, i, numOutputHandler);
        }

        disruptor.handleEventsWith(handlers);

        ringBuffer = disruptor.start();

        metricRegistry.register("output-buffer-remaining", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return ringBuffer.remainingCapacity();
            }
        });

    }

    public void publish(final ProcessedMessage processedMessage) {
        ringBuffer.publishEvent(new EventTranslator<Event>() {
            @Override
            public void translateTo(Event event, long sequence) {
                event.message = processedMessage;
            }
        });
    }

    public interface Factory {
        OutputBuffer create(@Assisted("filterSleepCalc") TimeCalculator outputTime,
                            @Assisted("bufferSize") int bufferSize, @Assisted("numOutputHandler") int numOutputHandler);
    }
}
