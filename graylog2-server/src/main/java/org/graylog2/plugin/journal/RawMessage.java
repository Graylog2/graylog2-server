/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin.journal;

import com.eaio.uuid.UUID;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.UninitializedMessageException;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.plugin.journal.JournalMessages.JournalMessage;

/**
 * A raw message is the unparsed data Graylog was handed by an input.
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

    private transient final JournalMessage.Builder msgBuilder;
    private final UUID id;
    private final long journalOffset;
    private Configuration codecConfig;

    public RawMessage(@Nonnull byte[] payload) {
        this(payload, (ResolvableInetSocketAddress)null);
    }

    public RawMessage(@Nonnull byte[] payload, @Nullable InetSocketAddress remoteAddress) {
        this(Long.MIN_VALUE, new UUID(), Tools.nowUTC(), ResolvableInetSocketAddress.wrap(remoteAddress), payload);
    }

    public RawMessage(@Nonnull byte[] payload, @Nullable ResolvableInetSocketAddress remoteAddress) {
        this(Long.MIN_VALUE, new UUID(), Tools.nowUTC(), remoteAddress, payload);
    }

    public RawMessage(long journalOffset,
                      @Nonnull UUID id,
                      DateTime timestamp,
                      @Nullable ResolvableInetSocketAddress remoteAddress,
                      @Nonnull byte[] payload) {
        checkNotNull(id, "The message id must not be null!");
        checkNotNull(payload, "The message payload must not be null!");
        if (payload.length == 0 && log.isTraceEnabled()) {
            log.trace("The message payload should not be empty, message {} from {} will be discarded.",
                      id,
                      remoteAddress == null ? "unknown" : remoteAddress,
                      new Throwable());
        }

        msgBuilder = JournalMessage.newBuilder();

        this.journalOffset = journalOffset;
        msgBuilder.setVersion(CURRENT_VERSION);

        this.id = id;
        msgBuilder.setUuidTime(id.time);
        msgBuilder.setUuidClockseq(id.clockSeqAndNode);

        msgBuilder.setTimestamp(timestamp.getMillis());
        if (null != remoteAddress) {
            setRemoteAddress(remoteAddress);
        }

        msgBuilder.setPayload(ByteString.copyFrom(payload));
    }

    public void addSourceNode(String sourceInputId, NodeId nodeId) {
        msgBuilder.addSourceNodesBuilder()
                  .setInputId(sourceInputId)
                  .setId(nodeId.toString())
                  .setType(JournalMessages.SourceNode.Type.SERVER)
                  .build();
    }

    public RawMessage(JournalMessage journalMessage, long journalOffset) {
        this.journalOffset = journalOffset;
        id = new UUID(journalMessage.getUuidTime(), journalMessage.getUuidClockseq());
        msgBuilder = JournalMessage.newBuilder(journalMessage);
        codecConfig = Configuration.deserializeFromJson(journalMessage.getCodec().getConfig());
    }

    @Nullable
    public static RawMessage decode(final byte[] buffer, final long journalOffset) {
        try {
            final JournalMessage journalMessage = JournalMessage.parseFrom(buffer);

            // TODO validate message based on field contents and version number

            return new RawMessage(journalMessage, journalOffset);
        } catch (IOException e) {
            log.error("Cannot read raw message from journal, ignoring this message.", e);
            return null;
        }
    }

    public byte[] encode() {
        try {
            final JournalMessages.CodecInfo codec = msgBuilder.getCodec();
            final JournalMessages.CodecInfo.Builder builder = JournalMessages.CodecInfo.newBuilder(codec);

            final String codecConfigJson = codecConfig.serializeToJson();
            if (codecConfigJson != null) {
                builder.setConfig(codecConfigJson);
            }
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
        return new DateTime(msgBuilder.getTimestamp(), DateTimeZone.UTC); // TODO PERFORMANCE object creation
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

    @Nullable
    public ResolvableInetSocketAddress getRemoteAddress() {
        if (msgBuilder.hasRemote()) {
            final JournalMessages.RemoteAddress address = msgBuilder.getRemote();
            final InetAddress inetAddr;
            try {
                inetAddr = InetAddress.getByAddress(address.getResolved(), address.getAddress().toByteArray());
            } catch (UnknownHostException e) {
                log.warn("Malformed InetAddress for message {}, expected 4 or 16 bytes, but got {} bytes",
                         id, address.getAddress().toByteArray());
                return null;
            }

            final int port = address.hasPort() ? address.getPort() : 0;
            // TODO PERFORMANCE object creation
            return ResolvableInetSocketAddress.wrap(new InetSocketAddress(inetAddr, port));
        }
        return null;
    }

    public void setRemoteAddress(ResolvableInetSocketAddress address) {
        final JournalMessages.RemoteAddress.Builder builder = msgBuilder.getRemoteBuilder();
        builder.setAddress(ByteString.copyFrom(address.getAddressBytes()))
                .setPort(address.getPort());

        // do not perform any reverse lookup here
        if (address.isReverseLookedUp()) {
            builder.setResolved(address.getHostName());
        }
    }

    public String getCodecName() {
        return msgBuilder.getCodecBuilder().getName();
    }

    public void setCodecName(String name) {
        checkArgument(!isNullOrEmpty(name), "The payload type must not be null or empty!");
        msgBuilder.getCodecBuilder().setName(name);
    }

    public Configuration getCodecConfig() {
        return codecConfig;
    }

    public void setCodecConfig(Configuration codecConfig) {
        this.codecConfig = codecConfig;
    }

    public List<SourceNode> getSourceNodes() {
        final ArrayList<SourceNode> list = Lists.newArrayList();

        for (final JournalMessages.SourceNode node : msgBuilder.getSourceNodesList()) {
            list.add(new SourceNode(node));
        }

        return list;
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        helper.add("id", getId())
                .add("journalOffset", getJournalOffset())
                .add("codec", getCodecName())
                .add("payloadSize", getPayload().length)
                .add("timestamp", getTimestamp());
        if (getRemoteAddress() != null) {
            helper.add("remoteAddress", getRemoteAddress().getInetSocketAddress().toString());
        }
        return helper.toString();
    }

    public long getJournalOffset() {
        return journalOffset;
    }

    public static class SourceNode {
        public String nodeId;
        public String inputId;
        public Type type;

        public enum Type {
            SERVER,
            @Deprecated
            RADIO
        }

        public SourceNode(JournalMessages.SourceNode node) {
            this.nodeId = node.getId();
            this.inputId = node.getInputId();

            switch (node.getType()) {
                case SERVER:
                    this.type = Type.SERVER;
                    break;
                case RADIO:
                    this.type = Type.RADIO;
                    break;
            }
        }
    }
}
