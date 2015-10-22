/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin.buffers;

import com.google.common.base.MoreObjects;
import com.lmax.disruptor.EventFactory;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.journal.RawMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

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
    private Collection<Message> messages;

    public boolean isSingleMessage() {
        return msg != null;
    }

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
    public Collection<Message> getMessages() {
        return messages;
    }

    public void setMessages(@Nullable final Collection<Message> messages) {
        this.messages = messages;
    }

    public void clearMessages() {
        setMessage(null);
        setMessages(null);
    }

    /**
     * Sets the raw message but also clears out the {@link #getMessage() message} and {@link #getMessages() messages}
     * references to avoid handling stale messages and to let older messages be garbage collected earlier.
     *
     * @param raw
     */
    public void setRaw(@Nonnull RawMessage raw) {
        this.raw = raw;
        clearMessages();
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
                .add("messages", messages)
                .toString();
    }
}
