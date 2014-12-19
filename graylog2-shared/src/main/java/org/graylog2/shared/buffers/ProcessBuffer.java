/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
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

    private final DecodingProcessor.Factory decodingProcessorFactory;
    private final ExecutorService executor;

    private final Meter incomingMessages;

    private final ServerStatus serverStatus;

    @Inject
    public ProcessBuffer(MetricRegistry metricRegistry,
                         ServerStatus serverStatus,
                         DecodingProcessor.Factory decodingProcessorFactory) {
        this.serverStatus = serverStatus;
        this.decodingProcessorFactory = decodingProcessorFactory;

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
    }

    private ExecutorService executorService(MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("processbufferprocessor-%d").build();
        return new InstrumentedExecutorService(Executors.newCachedThreadPool(threadFactory), metricRegistry);
    }

    public void initialize(ProcessBufferProcessor[] processors,
                           int ringBufferSize,
                           WaitStrategy waitStrategy) {
        this.ringBufferSize = ringBufferSize;
        Disruptor<MessageEvent> disruptor = new Disruptor<>(
                MessageEvent.EVENT_FACTORY,
                ringBufferSize,
                executor,
                ProducerType.MULTI,
                waitStrategy
        );

        LOG.info("Initialized ProcessBuffer with ring size <{}> "
                        + "and wait strategy <{}>.", ringBufferSize,
                waitStrategy.getClass().getSimpleName());

        disruptor.handleEventsWith(decodingProcessorFactory.create(decodeTime, parseTime)).then(processors);

        ringBuffer = disruptor.start();
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
