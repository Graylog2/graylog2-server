/*
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

package org.graylog2.inputs.syslog;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SyslogUDPDispatcher extends SyslogDispatcher {
    // Initialized with null to prevent memory allocation when no UDP input exists
    private static BlockingQueue<MessageEvent> blockingQueue = null;
    private static Thread[] threadPool = null;
    private static final Object lock = new Object();

    private final Meter dequeueRate;
    private final Meter enqueueRate;
    private final Timer processingTime;
    private final Timer enqueueTime;
    private final Meter queueFull;

    private class UDPQueueWorker implements Runnable {
        private final BlockingQueue<MessageEvent> blockingQueue;
        private final SyslogDispatcher syslogDispatcher;

        public UDPQueueWorker(BlockingQueue<MessageEvent> blockingQueue, SyslogDispatcher syslogDispatcher) {
            this.blockingQueue = blockingQueue;
            this.syslogDispatcher = syslogDispatcher;
        }

        @Override
        public void run() {
            while(true) {
                final MessageEvent evt;
                try {
                    evt = blockingQueue.take();
                    dequeueRate.mark();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                try(Timer.Context context = processingTime.time()) {
                    syslogDispatcher.processMessage(evt);
                } catch (BufferOutOfCapacityException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public SyslogUDPDispatcher(MetricRegistry metricRegistry, Buffer processBuffer, Configuration configuration, MessageInput sourceInput) {
        super(metricRegistry, processBuffer, configuration, sourceInput);
        this.dequeueRate = metricRegistry.meter(name(SyslogUDPDispatcher.class, "dequeueRate"));
        this.enqueueRate = metricRegistry.meter(name(SyslogUDPDispatcher.class, "enqueueRate"));
        this.processingTime = metricRegistry.timer(name(SyslogUDPDispatcher.class, "processingTime"));
        this.enqueueTime = metricRegistry.timer(name(SyslogUDPDispatcher.class, "enqueueTime"));
        this.queueFull = metricRegistry.meter(name(SyslogUDPDispatcher.class, "queueFull"));

        synchronized (lock) {
            LOG.info("Setting up UDP processing queue.");
            if (blockingQueue == null)
                blockingQueue = new ArrayBlockingQueue<>(100000);

            if (threadPool == null) {
                threadPool = new Thread[4];
                for (int i = 0; i < 4; i++) {
                    threadPool[i] = new Thread(new UDPQueueWorker(blockingQueue, this));
                    threadPool[i].setDaemon(true);
                    threadPool[i].start();
                }
            }
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent evt) throws Exception {
        try(Timer.Context context = this.enqueueTime.time()) {
            blockingQueue.add(evt);
            this.enqueueRate.mark();
        } catch (IllegalStateException e) {
            this.queueFull.mark();
        }
    }
}
