/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.buffers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.buffers.processors.DecodingProcessor;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;

public class ProcessBuffer extends Buffer {
    private final Timer parseTime;
    private final Timer decodeTime;

    private static final Logger LOG = LoggerFactory.getLogger(ProcessBuffer.class);

    public static String SOURCE_INPUT_ATTR_NAME;
    public static String SOURCE_NODE_ATTR_NAME;

    private final ExecutorService executor;

    private final Meter incomingMessages;

    @Inject
    public ProcessBuffer(MetricRegistry metricRegistry,
                         ServerStatus serverStatus,
                         DecodingProcessor.Factory decodingProcessorFactory,
                         Provider<ProcessBufferProcessor> bufferProcessorFactory,
                         @Named("processbuffer_processors") int processorCount,
                         @Named("ring_size") int ringSize,
                         @Named("processor_wait_strategy") String waitStrategyName) {
        this.ringBufferSize = ringSize;

        this.executor = executorService(metricRegistry);
        this.incomingMessages = metricRegistry.meter(name(ProcessBuffer.class, "incomingMessages"));

        this.parseTime = metricRegistry.timer(name(ProcessBuffer.class, "parseTime"));
        this.decodeTime = metricRegistry.timer(name(ProcessBuffer.class, "decodeTime"));

        if (serverStatus.hasCapability(ServerStatus.Capability.RADIO)) {
            SOURCE_INPUT_ATTR_NAME = "gl2_source_radio_input";
            SOURCE_NODE_ATTR_NAME = "gl2_source_radio";
        } else {
            SOURCE_INPUT_ATTR_NAME = "gl2_source_input";
            SOURCE_NODE_ATTR_NAME = "gl2_source_node";
        }

        final WaitStrategy waitStrategy = getWaitStrategy(waitStrategyName, "processor_wait_strategy");
        final Disruptor<MessageEvent> disruptor = new Disruptor<>(
                MessageEvent.EVENT_FACTORY,
                ringBufferSize,
                executor,
                ProducerType.MULTI,
                waitStrategy
        );
        disruptor.handleExceptionsWith(new LoggingExceptionHandler(LOG));

        LOG.info("Initialized ProcessBuffer with ring size <{}> "
                         + "and wait strategy <{}>.", ringBufferSize,
                 waitStrategy.getClass().getSimpleName());

        final ProcessBufferProcessor[] processors = new ProcessBufferProcessor[processorCount];
        for (int i = 0; i < processorCount; i++) {
            // TODO The provider should be converted to a factory so we can pass the DecodingProcessor instead of using a setter after object creation.
            processors[i] = bufferProcessorFactory.get();
            processors[i].setDecodingProcessor(decodingProcessorFactory.create(decodeTime, parseTime));
        }
        disruptor.handleEventsWithWorkerPool(processors);

        ringBuffer = disruptor.start();
    }

    private ExecutorService executorService(MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("processbufferprocessor-%d").build();
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
    }

    public void insertBlocking(@Nonnull RawMessage rawMessage) {
        final long sequence = ringBuffer.next();
        final MessageEvent event = ringBuffer.get(sequence);
        event.setRaw(rawMessage);
        ringBuffer.publish(sequence);
        afterInsert(1);
    }

    @Override
    protected void afterInsert(int n) {
        incomingMessages.mark(n);
    }

}
