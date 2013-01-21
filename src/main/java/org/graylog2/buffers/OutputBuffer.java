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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.graylog2.Core;
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.logmessage.LogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.MultiThreadedClaimStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBuffer implements Buffer {

    private static final Logger LOG = LoggerFactory.getLogger(OutputBuffer.class);
    
    protected static RingBuffer<LogMessagesEvent> ringBuffer;

    protected ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("outputbufferprocessor-%d")
                .build()
    );
    
    Core server;
    
    private final Meter incomingMessages = Metrics.newMeter(OutputBuffer.class, "InsertedMessages", "messages", TimeUnit.SECONDS);
    private final Meter rejectedMessages = Metrics.newMeter(OutputBuffer.class, "RejectedMessages", "messages", TimeUnit.SECONDS);

    public OutputBuffer(Core server) {
        this.server = server;
    }

    public void initialize() {
        Disruptor<LogMessagesEvent> disruptor = new Disruptor<LogMessagesEvent>(
                LogMessagesEvent.EVENT_FACTORY,
                executor,
                new MultiThreadedClaimStrategy(server.getConfiguration().getRingSize()),
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
    public void insert(LogMessage message) throws BufferOutOfCapacityException {
        throw new UnsupportedOperationException(
                "Method OutputBuffer.insert(message) is not supported");
    }

     public void insert(List<LogMessage> message) throws BufferOutOfCapacityException {
        if (!hasCapacity()) {
            LOG.warn("Rejecting message, because I am full. Raise my size or add more processors.");
            rejectedMessages.mark();
            throw new BufferOutOfCapacityException();
        }
        
        long sequence = ringBuffer.next();
        LogMessagesEvent event = ringBuffer.get(sequence);
        event.setMessages(message);
        ringBuffer.publish(sequence);

        server.outputBufferWatermark().incrementAndGet();
        incomingMessages.mark(message.size());
    }

    @Override
    public boolean hasCapacity() {
        return ringBuffer.remainingCapacity() > 0;
    }

}
