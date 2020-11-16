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
package org.graylog.plugins.beats;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.InflaterInputStream;

/**
 * FrameDecoder for the Beats/Lumberjack protocol.
 *
 * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md">Lumberjack protocol</a>
 */
public class BeatsFrameDecoder extends ReplayingDecoder<BeatsFrameDecoder.DecodingState> {
    private static final Logger LOG = LoggerFactory.getLogger(BeatsFrameDecoder.class);

    private static final byte PROTOCOL_VERSION = '2';
    private static final byte FRAME_ACK = 'A';
    private static final byte FRAME_COMPRESSED = 'C';
    private static final byte FRAME_DATA = 'D';
    private static final byte FRAME_JSON = 'J';
    private static final byte FRAME_WINDOW_SIZE = 'W';

    enum DecodingState {
        PROTOCOL_VERSION,
        FRAME_TYPE,
        FRAME_COMPRESSED,
        FRAME_DATA,
        FRAME_JSON,
        FRAME_WINDOW_SIZE
    }

    private long windowSize;
    private long sequenceNum;

    public BeatsFrameDecoder() {
        super(DecodingState.PROTOCOL_VERSION);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf buffer, List<Object> list) throws Exception {
        switch (state()) {
            case PROTOCOL_VERSION:
                checkVersion(buffer);
                checkpoint(DecodingState.FRAME_TYPE);
                // fall through
            case FRAME_TYPE:
                final byte frameType = buffer.readByte();
                switch (frameType) {
                    case FRAME_WINDOW_SIZE:
                        checkpoint(DecodingState.FRAME_WINDOW_SIZE);
                        break;
                    case FRAME_DATA:
                        checkpoint(DecodingState.FRAME_DATA);
                        break;
                    case FRAME_COMPRESSED:
                        checkpoint(DecodingState.FRAME_COMPRESSED);
                        break;
                    case FRAME_JSON:
                        checkpoint(DecodingState.FRAME_JSON);
                        break;
                    default:
                        throw new Exception("Unknown frame type: " + frameType);
                }
                return;
            case FRAME_WINDOW_SIZE:
                processWindowSizeFrame(buffer);
                break;
            case FRAME_DATA:
                list.addAll(parseDataFrame(channelHandlerContext.channel(), buffer));
                break;
            case FRAME_COMPRESSED:
                list.addAll(processCompressedFrame(channelHandlerContext.channel(), buffer));
                break;
            case FRAME_JSON:
                list.addAll(parseJsonFrame(channelHandlerContext.channel(), buffer));
                break;
            default:
                throw new Exception("Unknown decoding state: " + state());
        }

        checkpoint(DecodingState.PROTOCOL_VERSION);
    }

    private Collection<ByteBuf> processUncompressedBuffer(Channel channel, ByteBuf buffer) throws Exception {
        checkVersion(buffer);
        byte frameType = buffer.readByte();

        switch (frameType) {
            case FRAME_WINDOW_SIZE:
                processWindowSizeFrame(buffer);
                return Collections.emptyList();
            case FRAME_DATA:
                return parseDataFrame(channel, buffer);
            case FRAME_COMPRESSED:
                return processCompressedFrame(channel, buffer);
            case FRAME_JSON:
                return parseJsonFrame(channel, buffer);
            default:
                throw new Exception("Unknown frame type: " + frameType);
        }
    }

    private void checkVersion(ByteBuf channelBuffer) {
        byte version = channelBuffer.readByte();
        if (version != PROTOCOL_VERSION) {
            throw new IllegalStateException("Unknown beats protocol version: " + version);
        }
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#ack-frame-type">'ack' frame type</a>
     */
    private void sendACK(Channel channel) throws IOException {
        if (sequenceNum == windowSize) {
            final ByteBuf buffer = channel.alloc().buffer(6);
            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(FRAME_ACK);
            buffer.writeInt((int) sequenceNum);

            LOG.trace("Sending ACK for sequence number {} on channel {}", sequenceNum, channel);
            channel.writeAndFlush(buffer);
        }
    }

    /**
     * <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#json-frame-type">'json' frame type</a>
     */
    private Collection<ByteBuf> parseJsonFrame(Channel channel, ByteBuf channelBuffer) throws IOException {
        sequenceNum = channelBuffer.readUnsignedInt();
        LOG.trace("Received sequence number {}", sequenceNum);

        final int jsonLength = Ints.saturatedCast(channelBuffer.readUnsignedInt());

        final ByteBuf buffer = channelBuffer.readBytes(jsonLength);
        sendACK(channel);

        return Collections.singleton(buffer);
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#compressed-frame-type">'compressed' frame type</a>
     */
    private Collection<ByteBuf> processCompressedFrame(Channel channel, ByteBuf channelBuffer) throws Exception {
        final long payloadLength = channelBuffer.readUnsignedInt();
        final byte[] data = new byte[(int) payloadLength];
        channelBuffer.readBytes(data);
        try (final ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
             final InputStream in = new InflaterInputStream(dataStream)) {
            final ByteBuf buffer = Unpooled.wrappedBuffer(ByteStreams.toByteArray(in));
            return processCompressedDataFrames(channel, buffer);
        }
    }

    private Collection<ByteBuf> processCompressedDataFrames(Channel channel, ByteBuf channelBuffer) throws Exception {
        final List<ByteBuf> events = new ArrayList<>();
        while (channelBuffer.isReadable()) {
            final Collection<ByteBuf> buffers = processUncompressedBuffer(channel, channelBuffer);
            events.addAll(buffers);
        }
        return events;
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#window-size-frame-type">'window size' frame type</a>
     */
    private void processWindowSizeFrame(ByteBuf channelBuffer) {
        windowSize = channelBuffer.readUnsignedInt();
        LOG.trace("Changed window size to {}", windowSize);
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#data-frame-type">'data' frame type</a>
     */
    private Collection<ByteBuf> parseDataFrame(Channel channel, ByteBuf channelBuffer) throws IOException {
        sequenceNum = channelBuffer.readUnsignedInt();
        LOG.trace("Received sequence number {}", sequenceNum);

        final int pairs = Ints.saturatedCast(channelBuffer.readUnsignedInt());
        final JsonFactory jsonFactory = new JsonFactory();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (final JsonGenerator jg = jsonFactory.createGenerator(outputStream)) {
            jg.writeStartObject();
            for (int i = 0; i < pairs; i++) {
                final String key = parseDataItem(channelBuffer);
                final String value = parseDataItem(channelBuffer);
                jg.writeStringField(key, value);
            }
            jg.writeEndObject();
        }

        final ByteBuf buffer = Unpooled.wrappedBuffer(outputStream.toByteArray());
        sendACK(channel);

        return Collections.singleton(buffer);
    }

    private String parseDataItem(ByteBuf buf) {
        int length = Ints.saturatedCast(buf.readUnsignedInt());
        final ByteBuf item = buf.readSlice(length);
        return item.toString(StandardCharsets.UTF_8);
    }

    @VisibleForTesting
    long getWindowSize() {
        return windowSize;
    }

    @VisibleForTesting
    long getSequenceNum() {
        return sequenceNum;
    }
}
