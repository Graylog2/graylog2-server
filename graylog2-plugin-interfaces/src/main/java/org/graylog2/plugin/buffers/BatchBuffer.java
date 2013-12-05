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

import org.graylog2.plugin.Message;
import org.graylog2.plugin.inputs.MessageInput;

/**
 * @author James Furness
 */
public abstract class BatchBuffer extends Buffer {

    /**
     * Try to insert a batch of messages atomically if sufficient slots are available, failing without blocking or inserting
     * any messages if sufficient slots are not available.
     *
     * @throws BufferOutOfCapacityException if the buffer is bigger than the batch but insufficient free slots are available
     * @throws IllegalStateException        if the batch is bigger than the buffer, so an atomic insert is impossible
     */
    public abstract void insertFailFast(Message[] messages, MessageInput sourceInput) throws BufferOutOfCapacityException, ProcessingDisabledException;

    public boolean hasCapacity(int i) {
        return ringBuffer.remainingCapacity() >= i;
    }

}