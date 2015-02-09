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
package org.graylog2.shared.buffers;

import com.eaio.uuid.UUID;
import com.google.common.base.MoreObjects;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import org.graylog2.plugin.journal.RawMessage;

import java.nio.ByteBuffer;

public class RawMessageEvent {

    // the rawmessage will get be nulled as soon as the encoded raw message is being generated
    private RawMessage rawMessage;

    // once these fields are set, do NOT rely on rawMessage still being non-null!
    private byte[] messageIdBytes;
    private byte[] encodedRawMessage;

    public static final EventFactory<RawMessageEvent> FACTORY = new EventFactory<RawMessageEvent>() {
        @Override
        public RawMessageEvent newInstance() {
            return new RawMessageEvent();
        }
    };
    public static final EventTranslatorOneArg<RawMessageEvent, RawMessage> TRANSLATOR = new EventTranslatorOneArg<RawMessageEvent, RawMessage>() {
        @Override
        public void translateTo(RawMessageEvent event, long sequence, RawMessage arg0) {
            event.setRawMessage(arg0);
        }
    };

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("raw", getRawMessage())
                .add("uuid", getMessageId())
                .add("encodedLength", getEncodedRawMessage().length)
                .toString();
    }

    public RawMessage getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(RawMessage rawMessage) {
        this.rawMessage = rawMessage;
    }

    public byte[] getEncodedRawMessage() {
        return encodedRawMessage;
    }

    public void setEncodedRawMessage(byte[] encodedRawMessage) {
        this.encodedRawMessage = encodedRawMessage;
    }

    public void setMessageIdBytes(byte[] messageIdBytes) {
        this.messageIdBytes = messageIdBytes;
    }

    public byte[] getMessageIdBytes() {
        return messageIdBytes;
    }

    // performance doesn't matter, it's only being called during tracing
    public UUID getMessageId() {
        final ByteBuffer wrap = ByteBuffer.wrap(messageIdBytes);
        return new UUID(wrap.asLongBuffer().get(0), wrap.asLongBuffer().get(1));
    }
}
