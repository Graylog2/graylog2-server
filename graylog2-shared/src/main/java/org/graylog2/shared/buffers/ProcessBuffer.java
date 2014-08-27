/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.shared.buffers;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.inputs.Cache;
import org.graylog2.inputs.InputCache;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ProcessBuffer extends Buffer {
    public interface Factory {
        public ProcessBuffer create(InputCache inputCache, AtomicInteger processBufferWatermark);
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProcessBuffer.class);

    public static String SOURCE_INPUT_ATTR_NAME;
    public static String SOURCE_NODE_ATTR_NAME;

    protected ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("processbufferprocessor-%d")
                .build()
    );

    private final BaseConfiguration configuration;
    private final InputCache inputCache;
    private final AtomicInteger processBufferWatermark;

    private final Meter incomingMessages;
    private final Meter rejectedMessages;
    private final Meter cachedMessages;

    private final ServerStatus serverStatus;

    @AssistedInject
    public ProcessBuffer(MetricRegistry metricRegistry,
                         ServerStatus serverStatus,
                         BaseConfiguration configuration,
                         @Assisted InputCache inputCache,
                         @Assisted AtomicInteger processBufferWatermark) {
        this.serverStatus = serverStatus;
        this.configuration = configuration;
        this.inputCache = inputCache;
        this.processBufferWatermark = processBufferWatermark;

        incomingMessages = metricRegistry.meter(name(ProcessBuffer.class, "incomingMessages"));
        rejectedMessages = metricRegistry.meter(name(ProcessBuffer.class, "rejectedMessages"));
        cachedMessages = metricRegistry.meter(name(ProcessBuffer.class, "cachedMessages"));

        if (serverStatus.hasCapability(ServerStatus.Capability.RADIO)) {
            SOURCE_INPUT_ATTR_NAME = "gl2_source_radio_input";
            SOURCE_NODE_ATTR_NAME = "gl2_source_radio";
        } else {
            SOURCE_INPUT_ATTR_NAME = "gl2_source_input";
            SOURCE_NODE_ATTR_NAME = "gl2_source_node";
        }
    }

    public Cache getInputCache() {
        return inputCache;
    }

    public void initialize(ProcessBufferProcessor[] processors, int ringBufferSize, WaitStrategy waitStrategy, int processBufferProcessors) {
        Disruptor disruptor = new Disruptor<MessageEvent>(
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
        this.processBufferWatermark.addAndGet(n);
        incomingMessages.mark(n);
    }
}
