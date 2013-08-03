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
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.graylog2.Core;
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBuffer extends Buffer {

    private static final Logger LOG = LoggerFactory.getLogger(OutputBuffer.class);

    protected ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("outputbufferprocessor-%d")
                .build()
    );
    
    private Core server;
    
    private final Cache overflowCache;

    private final Meter incomingMessages;
    private final Meter rejectedMessages;
    private final Meter cachedMessages;

    public OutputBuffer(Core server, Cache overflowCache) {
        this.server = server;
        this.overflowCache = overflowCache;

        incomingMessages = server.metrics().meter(name(OutputBuffer.class, "incomingMessages"));
        rejectedMessages = server.metrics().meter(name(OutputBuffer.class, "rejectedMessages"));
        cachedMessages = server.metrics().meter(name(OutputBuffer.class, "cachedMessages"));
    }

    public void initialize() {
        Disruptor disruptor = new Disruptor<MessageEvent>(
                MessageEvent.EVENT_FACTORY,
                server.getConfiguration().getRingSize(),
                executor,
                ProducerType.MULTI,
                server.getConfiguration().getProcessorWaitStrategy()
        );
        
        LOG.info("Initialized OutputBuffer with ring size <{}> "
                + "and wait strategy <{}>.", server.getConfiguration().getRingSize(),
                server.getConfiguration().getProcessorWaitStrategy().getClass().getSimpleName());

        OutputBufferProcessor[] processors = new OutputBufferProcessor[server.getConfiguration().getOutputBufferProcessors()];
        
        for (int i = 0; i < server.getConfiguration().getOutputBufferProcessors(); i++) {
            processors[i] = new OutputBufferProcessor(this.server, i, server.getConfiguration().getOutputBufferProcessors());
        }
        
        disruptor.handleEventsWith(processors);
        
        ringBuffer = disruptor.start();
    }

    @Override
    public void insertCached(Message message, String sourceInputId) {
        if (!hasCapacity()) {
            LOG.debug("Out of capacity. Writing to cache.");
            cachedMessages.mark();
            overflowCache.add(message);
            return;
        }
        
        insert(message);
    }

    @Override
    public void insertFailFast(Message message, String sourceInputIds) throws BufferOutOfCapacityException {
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

        server.outputBufferWatermark().incrementAndGet();
        incomingMessages.mark();
    }

}
