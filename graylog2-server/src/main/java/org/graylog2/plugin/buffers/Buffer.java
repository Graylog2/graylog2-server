/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
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
package org.graylog2.plugin.buffers;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public abstract class Buffer {
    private static final Logger log = LoggerFactory.getLogger(Buffer.class);

    protected RingBuffer<MessageEvent> ringBuffer;
    protected int ringBufferSize;

    public boolean isEmpty() {
        return getUsage() == 0;
    }

    public long getRemainingCapacity() {
        return ringBuffer.remainingCapacity();
    }

    public int getRingBufferSize() {
        return ringBufferSize;
    }

    public long getUsage() {
        if (ringBuffer == null) {
            return 0;
        }
        return (long) ringBuffer.getBufferSize() - ringBuffer.remainingCapacity();
    }

    protected void insert(Message message) {
        long sequence = ringBuffer.next();
        MessageEvent event = ringBuffer.get(sequence);
        event.setMessage(message);
        ringBuffer.publish(sequence);

        afterInsert(1);

    }

    protected WaitStrategy getWaitStrategy(String waitStrategyName, String configOptionName) {
        switch (waitStrategyName) {
            case "sleeping":
                return new SleepingWaitStrategy();
            case "yielding":
                return new YieldingWaitStrategy();
            case "blocking":
                return new BlockingWaitStrategy();
            case "busy_spinning":
                return new BusySpinWaitStrategy();
            default:
                log.warn("Invalid setting for [{}]:"
                                + " Falling back to default: BlockingWaitStrategy.", configOptionName);
                return new BlockingWaitStrategy();
        }
    }

    protected abstract void afterInsert(int n);

    protected void insert(Message[] messages) {
        int length = messages.length;
        long hi = ringBuffer.next(length);
        long lo = hi - (length - 1);
        for (long sequence = lo; sequence <= hi; sequence++) {
            MessageEvent event = ringBuffer.get(sequence);
            event.setMessage(messages[(int)(sequence - lo)]);
        }
        ringBuffer.publish(lo, hi);
        afterInsert(length);
    }
}
