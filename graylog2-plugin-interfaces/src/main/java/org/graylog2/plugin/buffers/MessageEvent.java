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

import com.google.common.base.MoreObjects;
import com.lmax.disruptor.EventFactory;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.journal.RawMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MessageEvent {

    public final static EventFactory<MessageEvent> EVENT_FACTORY = new EventFactory<MessageEvent>() {
        @Override
        public MessageEvent newInstance()
        {
            return new MessageEvent();
        }
    };

    private RawMessage raw;
    private Message msg;
    private List<Message> msgList;

    @Nullable
    public Message getMessage()
    {
        return msg;
    }

    public void setMessage(@Nullable final Message msg)
    {
        this.msg = msg;
    }

    @Nullable
    public List<Message> getMessageList() {
        return msgList;
    }

    public void setMessageList(@Nullable final List<Message> msgList) {
        this.msgList = msgList;
    }

    /**
     * Sets the raw message but also clears out the {@link #getMessage() message} reference to avoid handling stale messages
     * and to let older messages be garbage collected earlier.
     *
     * @param raw
     */
    public void setRaw(@Nonnull RawMessage raw) {
        this.raw = raw;
        setMessage(null);
        setMessageList(null);
    }

    public void clearRaw() {
        this.raw = null;
    }

    @Nonnull
    public RawMessage getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("raw", raw)
                .add("message", msg)
                .add("messageList", msgList)
                .toString();
    }
}
