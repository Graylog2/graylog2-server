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
package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.lmax.disruptor.EventHandler;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public abstract class ProcessBufferProcessor implements EventHandler<MessageEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessBufferProcessor.class);

    protected AtomicInteger processBufferWatermark;
    private final Meter incomingMessages;
    private final Timer processTime;
    private final Meter outgoingMessages;

    protected final MetricRegistry metricRegistry;

    private final long ordinal;
    private final long numberOfConsumers;

    public ProcessBufferProcessor(MetricRegistry metricRegistry,
                                  AtomicInteger processBufferWatermark,
                                  final long ordinal,
                                  final long numberOfConsumers) {
        this.metricRegistry = metricRegistry;
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
        this.processBufferWatermark = processBufferWatermark;

        incomingMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "incomingMessages"));
        outgoingMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "outgoingMessages"));
        processTime = metricRegistry.timer(name(ProcessBufferProcessor.class, "processTime"));
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        // Because Trisha said so. (http://code.google.com/p/disruptor/wiki/FrequentlyAskedQuestions)
        if ((sequence % numberOfConsumers) != ordinal) {
            return;
        }

        processBufferWatermark.decrementAndGet();
        
        incomingMessages.mark();
        final Timer.Context tcx = processTime.time();

        Message msg = event.getMessage();

        LOG.debug("Starting to process message <{}>.", msg.getId());

        try {
            LOG.debug("Finished processing message <{}>. Writing to output buffer.", msg.getId());
            handleMessage(msg);
        } catch (Exception e) {
            LOG.warn("Unable to process message <{}>: {}", msg.getId(), e);
        } finally {
            outgoingMessages.mark();
            tcx.stop();
        }
    }

    protected abstract void handleMessage(Message msg);
}
