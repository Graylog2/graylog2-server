package org.graylog2.benchmarks.pipeline.singlebuffer;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.benchmarks.utils.TimeCalculator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageBuffer {

    private final MetricRegistry metricRegistry;
    private final FilterWorker.Factory filterWorkerFactory;
    private final OutputWorker.Factory outputWorkerFactory;
    private final int bufferSize;
    private final int numFilterHandler;
    private final int numOutputHandler;
    private final ExecutorService executor;
    private final Disruptor<Event> disruptor;
    private final RingBuffer<Event> ringBuffer;

    @AssistedInject
    public MessageBuffer(MetricRegistry metricRegistry,
                         FilterWorker.Factory filterWorkerFactory,
                         @Assisted("filterTime") TimeCalculator filterTime,
                         OutputWorker.Factory outputWorkerFactory,
                         @Assisted("outputTime") TimeCalculator outputTime,
                         @Assisted("bufferSize") int bufferSize,
                         @Assisted("numFilterHander") int numFilterHandler,
                         @Assisted("outputHander") int numOutputHandler) {
        this.metricRegistry = metricRegistry;
        this.filterWorkerFactory = filterWorkerFactory;
        this.outputWorkerFactory = outputWorkerFactory;
        this.bufferSize = bufferSize;
        this.numFilterHandler = numFilterHandler;
        this.numOutputHandler = numOutputHandler;

        executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("messagebuffer-%d")
                        .build()
        );

        EventFactory<Event> factory = System.getProperty("eventPadded") == null ? UnpaddedEvent.FACTORY : PaddedEvent.FACTORY;

        disruptor = new Disruptor<>(
                factory,
                bufferSize,
                executor,
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );

        final FilterWorker[] filters = new FilterWorker[numFilterHandler];
        for (int i = 0; i < numFilterHandler; i++) {
            filters[i] = filterWorkerFactory.create(filterTime, i);
        }

        final OutputWorker[] outputWorkers = new OutputWorker[numOutputHandler];
        for (int i = 0; i < numOutputHandler; i++) {
            outputWorkers[i] = outputWorkerFactory.create(outputTime, i);
        }

        disruptor.handleEventsWithWorkerPool(filters).thenHandleEventsWithWorkerPool(outputWorkers);

        ringBuffer = disruptor.start();

        metricRegistry.register("message-buffer-remaining", new Gauge<Long>() {
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
                event.setRawMessage(processedMessage);
            }
        });
    }

    public void stop() {
        disruptor.shutdown();
        executor.shutdown();
    }


    public interface Factory {
        MessageBuffer create(
                @Assisted("bufferSize") int bufferSize, @Assisted("filterTime") TimeCalculator filterTime,
                @Assisted("numFilterHander") int numFilterHandler, @Assisted("outputTime") TimeCalculator outputTime,
                @Assisted("outputHander") int numOutputHandler);
    }
}
