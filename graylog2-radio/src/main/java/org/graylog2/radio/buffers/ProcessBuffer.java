/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.radio.buffers;

import com.codahale.metrics.Meter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.inputs.Cache;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.Radio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ProcessBuffer extends Buffer {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessBuffer.class);

    public static final String SOURCE_RADIO_ATTR_NAME = "gl2_source_radio";
    public static final String SOURCE_RADIO_INPUT_ATTR_NAME = "gl2_source_radio_input";

    protected ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                    .setNameFormat("processbufferprocessor-%d")
                    .build()
    );

    private Radio radio;

    private final Cache masterCache;

    private final Meter incomingMessages;
    private final Meter rejectedMessages;
    private final Meter cachedMessages;

    public ProcessBuffer(Radio radio, Cache masterCache) {
        this.radio = radio;
        this.masterCache = masterCache;

        incomingMessages = radio.metrics().meter(name(ProcessBuffer.class, "incomingMessages"));
        rejectedMessages = radio.metrics().meter(name(ProcessBuffer.class, "rejectedMessages"));
        cachedMessages = radio.metrics().meter(name(ProcessBuffer.class, "cachedMessages"));
    }

    public void initialize() {
        Disruptor disruptor = new Disruptor<MessageEvent>(
                MessageEvent.EVENT_FACTORY,
                radio.getConfiguration().getRingSize(),
                executor,
                ProducerType.MULTI,
                radio.getConfiguration().getProcessorWaitStrategy()
        );

        LOG.info("Initialized ProcessBuffer with ring size <{}> "
                + "and wait strategy <{}>.", radio.getConfiguration().getRingSize(),
                radio.getConfiguration().getProcessorWaitStrategy().getClass().getSimpleName());

        ProcessBufferProcessor[] processors = new ProcessBufferProcessor[radio.getConfiguration().getProcessBufferProcessors()];

        for (int i = 0; i < radio.getConfiguration().getProcessBufferProcessors(); i++) {
            processors[i] = new ProcessBufferProcessor(radio, i, radio.getConfiguration().getProcessBufferProcessors());
        }

        disruptor.handleEventsWith(processors);

        ringBuffer = disruptor.start();
    }

    @Override
    public void insertCached(Message message, MessageInput sourceInput) {
        message.setSourceInput(sourceInput);

        message.addField(SOURCE_RADIO_INPUT_ATTR_NAME, sourceInput.getPersistId());
        message.addField(SOURCE_RADIO_ATTR_NAME, radio.getNodeId());

        if (!hasCapacity()) {
            LOG.debug("Out of capacity. Writing to cache.");
            cachedMessages.mark();
            masterCache.add(message);
            return;
        }

        insert(message);
    }

    @Override
    public void insertFailFast(Message message, MessageInput sourceInput) throws BufferOutOfCapacityException {
        message.setSourceInput(sourceInput);

        message.addField(SOURCE_RADIO_INPUT_ATTR_NAME, sourceInput.getId());
        message.addField(SOURCE_RADIO_ATTR_NAME, radio.getNodeId());

        if (!hasCapacity()) {
            LOG.debug("Rejecting message, because I am full and caching was disabled by input. Raise my size or add more processors.");
            rejectedMessages.mark();
            throw new BufferOutOfCapacityException();
        }

        insert(message);
    }

    private void insert(Message message) {
        long sequence = ringBuffer.next();
        MessageEvent event = ringBuffer.get(sequence);
        event.setMessage(message);
        ringBuffer.publish(sequence);

        radio.processBufferWatermark().incrementAndGet();
        incomingMessages.mark();
    }


}
