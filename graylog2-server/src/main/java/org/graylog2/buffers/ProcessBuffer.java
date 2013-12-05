/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2.buffers;

import com.codahale.metrics.Meter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.Core;
import org.graylog2.buffers.processors.ProcessBufferProcessor;
import org.graylog2.inputs.Cache;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.BatchBuffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ProcessBuffer extends BatchBuffer {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessBuffer.class);

    public static final String SOURCE_INPUT_ATTR_NAME = "gl2_source_input";
    public static final String SOURCE_NODE_ATTR_NAME = "gl2_source_node";

    private static final EventTranslatorOneArg<MessageEvent, Message> MESSAGE_EVENT_TRANSLATOR = new EventTranslatorOneArg<MessageEvent, Message>() {
        @Override
        public void translateTo(MessageEvent messageEvent, long l, Message o) {
            messageEvent.setMessage(o);
        }
    };

    protected ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                    .setNameFormat("processbufferprocessor-%d")
                    .build()
    );

    private Core server;

    private final Cache masterCache;

    private final Meter incomingMessages;
    private final Meter rejectedMessages;
    private final Meter cachedMessages;

    public ProcessBuffer(Core server, Cache masterCache) {
        this.server = server;
        this.masterCache = masterCache;

        incomingMessages = server.metrics().meter(name(ProcessBuffer.class, "incomingMessages"));
        rejectedMessages = server.metrics().meter(name(ProcessBuffer.class, "rejectedMessages"));
        cachedMessages = server.metrics().meter(name(ProcessBuffer.class, "cachedMessages"));
    }

    public void initialize() {
        Disruptor disruptor = new Disruptor<MessageEvent>(
                MessageEvent.EVENT_FACTORY,
                server.getConfiguration().getRingSize(),
                executor,
                ProducerType.MULTI,
                server.getConfiguration().getProcessorWaitStrategy()
        );

        LOG.info("Initialized ProcessBuffer with ring size <{}> "
                + "and wait strategy <{}>.", server.getConfiguration().getRingSize(),
                server.getConfiguration().getProcessorWaitStrategy().getClass().getSimpleName());

        ProcessBufferProcessor[] processors = new ProcessBufferProcessor[server.getConfiguration().getProcessBufferProcessors()];

        for (int i = 0; i < server.getConfiguration().getProcessBufferProcessors(); i++) {
            processors[i] = new ProcessBufferProcessor(this.server, i, server.getConfiguration().getProcessBufferProcessors());
        }

        disruptor.handleEventsWith(processors);

        ringBuffer = disruptor.start();
    }

    @Override
    public void insertCached(Message message, MessageInput sourceInput) {
        message.setSourceInput(sourceInput);

        message.addField(SOURCE_INPUT_ATTR_NAME, sourceInput.getId());
        message.addField(SOURCE_NODE_ATTR_NAME, server.getNodeId());

        if (!server.isProcessing()) {
            LOG.debug("Message processing is paused. Writing to cache.");
            cachedMessages.mark();
            masterCache.add(message);
            return;
        }

        if (!hasCapacity()) {
            LOG.debug("Out of capacity. Writing to cache.");
            cachedMessages.mark();
            masterCache.add(message);
            return;
        }

        insert(message);
    }

    @Override
    public void insertFailFast(Message message, MessageInput sourceInput) throws BufferOutOfCapacityException, ProcessingDisabledException {
        message.setSourceInput(sourceInput);

        message.addField(SOURCE_INPUT_ATTR_NAME, sourceInput.getId());
        message.addField(SOURCE_NODE_ATTR_NAME, server.getNodeId());

        if (!server.isProcessing()) {
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
    public void insertFailFast(Message[] messages, MessageInput sourceInput) throws BufferOutOfCapacityException, ProcessingDisabledException {
        if (ringBuffer.getBufferSize() < messages.length) {
            throw new IllegalStateException("Message batch size too large for atomic bulk insert to be possible - RingBuffer size (" + ringBuffer.getBufferSize() + ") < message batch size (" + messages.length + ")");
        }

        if (!hasCapacity(messages.length)) {
            LOG.warn("Rejecting message, because I am full. Raise my size or add more processors.");
            rejectedMessages.mark(messages.length);
            throw new BufferOutOfCapacityException();
        }

        for (Message message : messages) {
            message.setSourceInput(sourceInput);

            message.addField(SOURCE_INPUT_ATTR_NAME, sourceInput.getId());
            message.addField(SOURCE_NODE_ATTR_NAME, server.getNodeId());
        }

        boolean published = ringBuffer.tryPublishEvents(MESSAGE_EVENT_TRANSLATOR, messages);

        if (published) {
            server.processBufferWatermark().addAndGet(messages.length);
            incomingMessages.mark(messages.length);

        } else {
            LOG.warn("Rejecting message, because I am full. Raise my size or add more processors.");
            rejectedMessages.mark(messages.length);
            throw new BufferOutOfCapacityException();

        }
    }

    private void insert(Message message) {
        ringBuffer.publishEvent(MESSAGE_EVENT_TRANSLATOR, message);

        server.processBufferWatermark().incrementAndGet();
        incomingMessages.mark();
    }

}
