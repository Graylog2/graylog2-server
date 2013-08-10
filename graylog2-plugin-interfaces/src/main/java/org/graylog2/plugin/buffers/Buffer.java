/**
 * Copyright (c) 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
*/
package org.graylog2.plugin.buffers;

import com.lmax.disruptor.RingBuffer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.inputs.MessageInput;

/**
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public abstract class Buffer {

    protected RingBuffer<MessageEvent> ringBuffer;

    public abstract void insertFailFast(Message message, MessageInput sourceInput) throws BufferOutOfCapacityException, ProcessingDisabledException;
    public abstract void insertCached(Message message, MessageInput sourceInput);

    public boolean isEmpty() {
        return getUsage() == 0;
    }

    public boolean hasCapacity() {
        return ringBuffer.remainingCapacity() > 0;
    }

    public long getUsage() {
        return Long.valueOf(ringBuffer.getBufferSize())-ringBuffer.remainingCapacity();
    }
    
}
