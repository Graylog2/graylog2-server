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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.MultiThreadedClaimStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.graylog2.Core;
import org.graylog2.buffers.processors.ProcessBufferProcessor;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.logmessage.LogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ProcessBuffer implements Buffer {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessBuffer.class);
    
    protected static RingBuffer<LogMessageEvent> ringBuffer;

    protected ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("processbufferprocessor-%d")
                .build()
    );

    Core server;
    
    private final Meter incomingMessages = Metrics.newMeter(ProcessBuffer.class, "InsertedMessages", "messages", TimeUnit.SECONDS);
    private final Meter rejectedMessages = Metrics.newMeter(ProcessBuffer.class, "RejectedMessages", "messages", TimeUnit.SECONDS);

    public ProcessBuffer(Core server) {
        this.server = server;
    }

    public void initialize() {
        Disruptor disruptor = new Disruptor<LogMessageEvent>(
                LogMessageEvent.EVENT_FACTORY,
                executor,
                new MultiThreadedClaimStrategy(server.getConfiguration().getRingSize()),
                new SleepingWaitStrategy()
        );

        ProcessBufferProcessor[] processors = new ProcessBufferProcessor[server.getConfiguration().getProcessBufferProcessors()];
        
        for (int i = 0; i < server.getConfiguration().getProcessBufferProcessors(); i++) {
            processors[i] = new ProcessBufferProcessor(this.server, i, server.getConfiguration().getProcessBufferProcessors());
        }
        
        disruptor.handleEventsWith(processors);
        
        ringBuffer = disruptor.start();
    }
    
    @Override
    public void insert(LogMessage message) {
        if (ringBuffer.remainingCapacity() > 0) {
            long sequence = ringBuffer.next();
            LogMessageEvent event = ringBuffer.get(sequence);
            event.setMessage(message);
            ringBuffer.publish(sequence);

            server.processBufferWatermark().incrementAndGet();
            incomingMessages.mark();
        } else {
            LOG.error("ProcessBuffer is out of capacity. Raise the ring_size configuration parameter. DROPPING MESSAGE!");
            rejectedMessages.mark();
        }
    }

}
