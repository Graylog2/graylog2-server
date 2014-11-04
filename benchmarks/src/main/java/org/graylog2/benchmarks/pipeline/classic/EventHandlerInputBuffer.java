package org.graylog2.benchmarks.pipeline.classic;

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

public class EventHandlerInputBuffer {

    protected final RingBuffer<Event> ringBuffer;
    protected final ExecutorService executor;
    protected final Disruptor<Event> disruptor;

    @AssistedInject
    public EventHandlerInputBuffer(MetricRegistry metricRegistry,
                                   FilterHandler.Factory handlerFactory,
                                   @Assisted TimeCalculator filterTime,
                                   @Assisted OutputBuffer outputBuffer,
                                   @Assisted("bufferSize") int bufferSize,
                                   @Assisted("numFilterHander") int numFilterHandler) {
        executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("inputbuffer-%d")
                        .build()
        );

        disruptor = new Disruptor<>(
                Event.FACTORY,
                bufferSize,
                executor,
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );

        setupHandlers(handlerFactory, filterTime, outputBuffer, numFilterHandler);

        ringBuffer = disruptor.start();

        metricRegistry.register("input-buffer-remaining", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return ringBuffer.remainingCapacity();
            }
        });
    }

    protected void setupHandlers(FilterHandler.Factory handlerFactory, TimeCalculator filterTime, OutputBuffer outputBuffer, int numFilterHandler) {
        final FilterHandler[] handlers = new FilterHandler[numFilterHandler];
        for (int i = 0; i < numFilterHandler; i++) {
            handlers[i] = handlerFactory.create(outputBuffer, filterTime, i, numFilterHandler);
        }

        disruptor.handleEventsWith(handlers);
    }

    public void publish(final ProcessedMessage processedMessage) {
        ringBuffer.publishEvent(new EventTranslator<Event>() {
            @Override
            public void translateTo(Event event, long sequence) {
                event.message = processedMessage;
            }
        });
    }

    public void stop() {
        disruptor.shutdown();
        executor.shutdown();
    }

    public interface Factory {
        EventHandlerInputBuffer create(TimeCalculator filterTime,
                                       @Assisted OutputBuffer outputBuffer,
                                       @Assisted("bufferSize") int bufferSize,
                                       @Assisted("numFilterHander") int numFilterHandler);
    }
}
