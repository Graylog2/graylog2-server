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
import com.codahale.metrics.InstrumentedThreadFactory;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.inputs.Cache;
import org.graylog2.inputs.InputCache;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;

public class ProcessBuffer extends Buffer {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessBuffer.class);

    public static String SOURCE_INPUT_ATTR_NAME;
    public static String SOURCE_NODE_ATTR_NAME;

    private final BaseConfiguration configuration;
    private final InputCache inputCache;
    private final ExecutorService executor;

    private final Meter incomingMessages;
    private final Meter rejectedMessages;
    private final Meter cachedMessages;

    private final ServerStatus serverStatus;

    @Inject
    public ProcessBuffer(MetricRegistry metricRegistry,
                         ServerStatus serverStatus,
                         BaseConfiguration configuration,
                         InputCache inputCache) {
        this.serverStatus = serverStatus;
        this.configuration = configuration;
        this.inputCache = inputCache;

        this.executor = executorService(metricRegistry);
        this.incomingMessages = metricRegistry.meter(name(ProcessBuffer.class, "incomingMessages"));
        this.rejectedMessages = metricRegistry.meter(name(ProcessBuffer.class, "rejectedMessages"));
        this.cachedMessages = metricRegistry.meter(name(ProcessBuffer.class, "cachedMessages"));

        if (serverStatus.hasCapability(ServerStatus.Capability.RADIO)) {
            SOURCE_INPUT_ATTR_NAME = "gl2_source_radio_input";
            SOURCE_NODE_ATTR_NAME = "gl2_source_radio";
        } else {
            SOURCE_INPUT_ATTR_NAME = "gl2_source_input";
            SOURCE_NODE_ATTR_NAME = "gl2_source_node";
        }
    }

    private ExecutorService executorService(MetricRegistry metricRegistry) {
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory(metricRegistry)), metricRegistry);
    }

    private ThreadFactory threadFactory(MetricRegistry metricRegistry) {
        return new InstrumentedThreadFactory(
                new ThreadFactoryBuilder().setNameFormat("processbufferprocessor-%d").build(), metricRegistry);
    }

    public Cache getInputCache() {
        return inputCache;
    }

    public void initialize(ProcessBufferProcessor[] processors, int ringBufferSize, WaitStrategy waitStrategy, int processBufferProcessors) {
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

        disruptor.handleEventsWith(processors);

        ringBuffer = disruptor.start();
    }

    @Override
    public void insertCached(Message message, MessageInput sourceInput) {
        prepareMessage(message, sourceInput);

        if (!serverStatus.isProcessing()) {
            LOG.debug("Message processing is paused. Writing to cache.");
            cachedMessages.mark();
            inputCache.add(message);
            return;
        }

        if (!hasCapacity()) {
            if (configuration.getInputCacheMaxSize() == 0 || inputCache.size() < configuration.getInputCacheMaxSize()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Out of capacity. Writing to cache.");
                cachedMessages.mark();
                inputCache.add(message);
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("Out of capacity. Input cache limit reached. Dropping message.");
                rejectedMessages.mark();
            }
            return;
        }

        insert(message);
    }

    private void prepareMessage(Message message, MessageInput sourceInput) {
        message.setSourceInput(sourceInput);

        final String source_input_name;

        if (sourceInput != null)
            source_input_name = sourceInput.getId();
        else
            source_input_name = "<nonexistent input>";

        message.addField(SOURCE_INPUT_ATTR_NAME, source_input_name);
        message.addField(SOURCE_NODE_ATTR_NAME, serverStatus.getNodeId());
    }

    @Override
    public void insertFailFast(Message message, MessageInput sourceInput) throws BufferOutOfCapacityException, ProcessingDisabledException {
        prepareMessage(message, sourceInput);

        if (!serverStatus.isProcessing()) {
            LOG.debug("Rejecting message, because message processing is paused.");
            throw new ProcessingDisabledException();
        }

        if (!hasCapacity()) {
            LOG.debug("Rejecting message, because I am full and caching was disabled by input. Raise my size or add more processors.");
            rejectedMessages.mark();
            throw new BufferOutOfCapacityException();
        }

        insert(message);
    }

    @Override
    public void insertFailFast(List<Message> messages) throws BufferOutOfCapacityException, ProcessingDisabledException {
        int length = messages.size();
        for (Message message : messages) {
            MessageInput sourceInput = message.getSourceInput();
            prepareMessage(message, sourceInput);
        }

        if (!serverStatus.isProcessing()) {
            LOG.debug("Rejecting message, because message processing is paused.");
            throw new ProcessingDisabledException();
        }

        if (!hasCapacity(length)) {
            LOG.debug("Rejecting message, because I am full and caching was disabled by input. Raise my size or add more processors.");
            rejectedMessages.mark();
            throw new BufferOutOfCapacityException();
        }

        insert(messages.toArray(new Message[length]));
        afterInsert(length);
    }

    @Override
    public void insertCached(List<Message> messages) {
        int length = messages.size();
        for (Message message : messages)
            prepareMessage(message, message.getSourceInput());

        if (!serverStatus.isProcessing()) {
            LOG.debug("Message processing is paused. Writing to cache.");
            cachedMessages.mark();
            inputCache.add(messages);
            return;
        }

        if (!hasCapacity(length)) {
            if (configuration.getInputCacheMaxSize() == 0 || inputCache.size() < configuration.getInputCacheMaxSize()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Out of capacity. Writing to cache.");
                cachedMessages.mark();
                inputCache.add(messages);
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("Out of capacity. Input cache limit reached. Dropping message.");
                rejectedMessages.mark();
            }
            return;
        }

        insert(messages.toArray(new Message[length]));
        afterInsert(length);
    }

    @Override
    protected void afterInsert(int n) {
        incomingMessages.mark(n);
    }
}
