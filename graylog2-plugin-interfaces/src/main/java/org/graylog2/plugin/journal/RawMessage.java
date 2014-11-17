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
package org.graylog2.plugin.journal;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.protobuf.ByteString;
import com.google.protobuf.UninitializedMessageException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.JournalMessages.SourceNode;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.plugin.journal.JournalMessages.JournalMessage;

/**
 * A raw message is the unparsed data Graylog2 was handed by an input.
 * <p>
 * Typically this is a copy of the exact bytes received over the network, after all de-chunking, removal of transport
 * headers, etc has been performed, but before any parsing, decoding, checking of the actual payload has been performed.
 * </p>
 * <p>
 * Each raw message has a unique id, a timestamp it was received at (this might be different from the timestamp contained
 * in the payload, if that has any), a tag of what payload type this is supposed to be (e.g. syslog, GELF, RadioMessage etc.),
 * as well as an opaque meta data structure.<br>
 * The format of the meta data is not stable yet, but will likely be a JSON string.
 * </p>
 */
public class RawMessage implements Serializable {
    public static final byte CURRENT_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(RawMessage.class);

    private final JournalMessage.Builder msgBuilder;
    private final UUID id;
    private final long sequenceNumber;
    private Configuration codecConfig;

    public RawMessage(String payloadType,
                      String sourceInputId,
                      InetSocketAddress remoteAddress,
                      byte[] payload) {
        this(payloadType, sourceInputId, remoteAddress, null, payload);
    }

    public RawMessage(String payloadType,
                      String sourceInputId,
                      InetSocketAddress remoteAddress,
                      @Nullable String metaData,
                      byte[] payload) {
        this(Long.MIN_VALUE, new UUID(), Tools.iso8601(), payloadType, sourceInputId, remoteAddress, metaData, payload);
    }

    public RawMessage(long sequenceNumber,
                      UUID id,
                      DateTime timestamp,
                      String payloadType,
                      String sourceInputId,
                      InetSocketAddress remoteAddress,
                      @Nullable String metaData,
                      byte[] payload) {
        checkNotNull(payload, "The message payload must not be null!");
        checkArgument(payload.length > 0, "The message payload must not be empty!");
        checkArgument(!isNullOrEmpty(payloadType), "The payload type must not be null or empty!");

        msgBuilder = JournalMessage.newBuilder();

        this.sequenceNumber = sequenceNumber;
        msgBuilder.setVersion(CURRENT_VERSION);

        this.id = id;
        msgBuilder.setUuidTime(id.time);
        msgBuilder.setUuidClockseq(id.clockSeqAndNode);

        msgBuilder.setTimestamp(timestamp.getMillis());
        msgBuilder.addSourceNodes(
                msgBuilder.addSourceNodesBuilder()
                        .setId(sourceInputId)
                        .setType(SourceNode.Type.SERVER)
                        .build()
        );
        if (null != remoteAddress) {
            final JournalMessages.RemoteAddress.Builder remoteBuilder = msgBuilder.getRemoteBuilder()
                    .setAddress(ByteString.copyFrom(remoteAddress.getAddress().getAddress()))
                    .setPort(remoteAddress.getPort());
            // don't resolve the address just for serializing it. callers will decide whether to resolve addresses early or not.
            if (!remoteAddress.isUnresolved()) {
                remoteBuilder.setResolved(remoteAddress.getHostName());
            }
            msgBuilder.setRemote(remoteBuilder.build());
        }

        msgBuilder.setPayload(ByteString.copyFrom(payload));

        final JournalMessages.CodecInfo.Builder codecBuilder = msgBuilder.getCodecBuilder();
        codecBuilder.setName(payloadType);
        if (metaData != null) {
            codecBuilder.setConfig(metaData);
        }
        msgBuilder.setCodec(codecBuilder.build());
    }

    public RawMessage(JournalMessage journalMessage, long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
        id = new UUID(journalMessage.getUuidTime(), journalMessage.getUuidClockseq());
        msgBuilder = JournalMessage.newBuilder(journalMessage);
        codecConfig = Configuration.deserializeFromJson(journalMessage.getCodec().getConfig());
    }

    public static RawMessage decode(final ByteBuffer buffer, final long sequenceNumber) {
        try {
            final JournalMessage journalMessage = JournalMessage.parseFrom(new ByteBufferBackedInputStream(buffer));

            // TODO validate message based on field contents and version number

            return new RawMessage(journalMessage, sequenceNumber);
        } catch (IOException e) {
            log.error("Cannot read raw message from journal, ignoring this message.", e);
            return null;
        }
    }

    public byte[] encode() {
        try {
            final JournalMessages.CodecInfo codec = msgBuilder.getCodec();
            final JournalMessages.CodecInfo.Builder builder = JournalMessages.CodecInfo.newBuilder(codec);

            builder.setConfig(codecConfig.serializeToJson());
            msgBuilder.setCodec(builder.build());

            final JournalMessage journalMessage = msgBuilder.build();
            return journalMessage.toByteArray();
        } catch (UninitializedMessageException e) {
            log.error(
                    "Unable to write RawMessage to journal because required fields are missing, " +
                            "this message will be discarded. This is a bug.", e);
            return null;
        }
    }

    public int getVersion() {
        return msgBuilder.getVersion();
    }

    public DateTime getTimestamp() {
        return new DateTime(msgBuilder.getTimestamp()); // TODO PERFORMANCE object creation
    }

    public String getPayloadType() {
        return msgBuilder.getCodec().getName();
    }

    public byte[] getPayload() {
        return msgBuilder.getPayload().toByteArray(); // TODO PERFORMANCE array copy
    }

    public UUID getId() {
        return id;
    }

    public byte[] getIdBytes() {
        final long time = id.getTime();
        final long clockSeqAndNode = id.getClockSeqAndNode();

        return ByteBuffer.allocate(16)
                .putLong(time)
                .putLong(clockSeqAndNode)
                .array(); // TODO PERFORMANCE object creation
    }

    public InetAddress getRemoteAddress() {
        // TODO return InetSocketAddress here!
        if (msgBuilder.hasRemote()) {
            final JournalMessages.RemoteAddress remoteAddress = msgBuilder.getRemote();
            try {
                return InetAddress.getByAddress(remoteAddress.getResolved(),
                                         remoteAddress.getAddress().toByteArray());
            } catch (UnknownHostException e) {
                log.error("Cannot get remote address of raw message", e);
            }
        }
        return null;
    }

    public RawMessage setRemoteAddress(InetAddress address) {
        // TODO return InetSocketAddress here!
        final JournalMessages.RemoteAddress.Builder remoteBuilder = msgBuilder.getRemoteBuilder();
        remoteBuilder.setAddress(ByteString.copyFrom(address.getAddress()));

        msgBuilder.setRemote(remoteBuilder.build());
        return this;
    }

    public Configuration getCodecConfig() {
        return codecConfig;
    }

    public void setCodecConfig(Configuration codecConfig) {
        this.codecConfig = codecConfig;
    }
}
