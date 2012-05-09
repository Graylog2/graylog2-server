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

import com.lmax.disruptor.MultiThreadedClaimStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.graylog2.GraylogServer;
import org.graylog2.buffers.processors.ProcessBufferProcessor;
import org.graylog2.logmessage.LogMessage;

/**
 * ProcessBuffer.java: 17.04.2012 12:21:29
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ProcessBuffer {

    protected static final int RING_SIZE = 8192;
    protected static RingBuffer<LogMessageEvent> ringBuffer;

    protected ExecutorService executor = Executors.newCachedThreadPool();

    GraylogServer server;

    public ProcessBuffer(GraylogServer server) {
        this.server = server;
    }

    public void initialize() {
        Disruptor disruptor = new Disruptor<LogMessageEvent>(
                LogMessageEvent.EVENT_FACTORY,
                executor,
                new MultiThreadedClaimStrategy(RING_SIZE),
                new SleepingWaitStrategy()
        );

        ProcessBufferProcessor processor = new ProcessBufferProcessor(this.server);

        disruptor.handleEventsWith(processor);
        ringBuffer = disruptor.start();
    }
    
    public void insert(LogMessage message) {
        long sequence = ringBuffer.next();
        LogMessageEvent event = ringBuffer.get(sequence);
        event.setMessage(message);
        ringBuffer.publish(sequence);
    }

}
