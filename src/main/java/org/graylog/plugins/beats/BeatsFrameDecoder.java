/**
 * This file is part of Graylog Beats Plugin.
 *
 * Graylog Beats Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Beats Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Beats Plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.beats;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import static java.util.Objects.requireNonNull;

/**
 * FrameDecoder for the Beats/Lumberjack protocol.
 *
 * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md">Lumberjack protocol</a>
 */
public class BeatsFrameDecoder extends FrameDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(BeatsFrameDecoder.class);

    private static final byte PROTOCOL_VERSION = '2';
    private static final byte FRAME_ACK = 'A';
    private static final byte FRAME_COMPRESSED = 'C';
    private static final byte FRAME_DATA = 'D';
    private static final byte FRAME_JSON = 'J';
    private static final byte FRAME_WINDOW_SIZE = 'W';

    private final ObjectMapper objectMapper;
    private long windowSize;
    private long sequenceNum;

    public BeatsFrameDecoder(ObjectMapper objectMapper) {
        this.objectMapper = requireNonNull(objectMapper);
    }

    @Override
    protected Object decode(ChannelHandlerContext channelHandlerContext, Channel channel, ChannelBuffer channelBuffer) throws Exception {
        final List<Map<String, Object>> events = processBuffer(channel, channelBuffer);

        if (events == null) {
            return null;
        } else {
            return ChannelBuffers.copiedBuffer(objectMapper.writeValueAsBytes(events));
        }
    }

    @Nullable
    private List<Map<String, Object>> processBuffer(Channel channel, ChannelBuffer channelBuffer) throws IOException {
        channelBuffer.markReaderIndex();
        @SuppressWarnings("unused")
        byte version = channelBuffer.readByte();
        if (LOG.isTraceEnabled() && version != PROTOCOL_VERSION) {
            LOG.trace("Unknown beats protocol version: {}", version);
        }
        byte frameType = channelBuffer.readByte();

        List<Map<String, Object>> events = null;
        switch (frameType) {
            case FRAME_WINDOW_SIZE:
                processWindowSizeFrame(channelBuffer);
                break;
            case FRAME_DATA:
                events = Collections.singletonList(parseDataFrame(channelBuffer));
                sendACK(channel);
                break;
            case FRAME_COMPRESSED:
                events = processCompressedFrame(channel, channelBuffer);
                break;
            case FRAME_JSON:
                events = Collections.singletonList(parseJsonFrame(channelBuffer));
                sendACK(channel);
                break;
            default:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cannot understand frame type: {}", Character.getName(frameType));
                }
                break;
        }
        return events;
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#ack-frame-type">'ack' frame type</a>
     */
    private void sendACK(Channel channel) throws IOException {
        if (sequenceNum == windowSize) {
            final ChannelBuffer buffer = ChannelBuffers.buffer(6);
            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(FRAME_ACK);
            buffer.writeInt((int) sequenceNum);

            LOG.trace("Sending ACK for sequence number {} on channel {}", sequenceNum, channel);
            channel.write(buffer).awaitUninterruptibly();
        }
    }

    /**
     * <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#json-frame-type">'json' frame type</a>
     */
    private Map<String, Object> parseJsonFrame(ChannelBuffer channelBuffer) throws IOException {
        sequenceNum = channelBuffer.readUnsignedInt();
        LOG.trace("Received sequence number {}", sequenceNum);

        final int jsonLength = Ints.saturatedCast(channelBuffer.readUnsignedInt());
        final byte[] data = new byte[jsonLength];
        channelBuffer.readBytes(data);
        return objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#compressed-frame-type">'compressed' frame type</a>
     */
    private List<Map<String, Object>> processCompressedFrame(Channel channel, ChannelBuffer channelBuffer) throws IOException {
        if (channelBuffer.readableBytes() >= 4) {
            final long payloadLength = channelBuffer.readUnsignedInt();
            if (channelBuffer.readableBytes() < payloadLength) {
                channelBuffer.resetReaderIndex();
            } else {
                final byte[] data = new byte[(int) payloadLength];
                channelBuffer.readBytes(data);
                try (final ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
                     final InputStream in = new InflaterInputStream(dataStream)) {
                    final ChannelBuffer buffer = ChannelBuffers.copiedBuffer(ByteStreams.toByteArray(in));
                    return processCompressedDataFrames(channel, buffer);
                }
            }
        } else {
            channelBuffer.resetReaderIndex();
        }
        return null;
    }

    private List<Map<String, Object>> processCompressedDataFrames(Channel channel, ChannelBuffer channelBuffer) throws IOException {
        final List<Map<String, Object>> events = new ArrayList<>();
        while (channelBuffer.readable()) {
            final List<Map<String, Object>> buffer = processBuffer(channel, channelBuffer);
            if (buffer != null) {
                events.addAll(buffer);
            }
        }
        return events;
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#window-size-frame-type">'window size' frame type</a>
     */
    private void processWindowSizeFrame(ChannelBuffer channelBuffer) {
        if (channelBuffer.readableBytes() < 4) {
            channelBuffer.resetReaderIndex();
        } else {
            windowSize = channelBuffer.readUnsignedInt();
            LOG.trace("Changed window size to {}", windowSize);
        }
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#data-frame-type">'data' frame type</a>
     */
    private Map<String, Object> parseDataFrame(ChannelBuffer channelBuffer) {
        sequenceNum = channelBuffer.readUnsignedInt();
        LOG.trace("Received sequence number {}", sequenceNum);

        int pairs = Ints.saturatedCast(channelBuffer.readUnsignedInt());
        final Map<String, Object> data = new HashMap<>(pairs);
        for (int i = 0; i < pairs; i++) {
            final String key = parseDataItem(channelBuffer);
            final String value = parseDataItem(channelBuffer);
            data.put(key, value);
        }

        return data;
    }

    private String parseDataItem(ChannelBuffer channelBuffer) {
        long length = channelBuffer.readUnsignedInt();
        final byte[] bytes = new byte[(int) length];
        channelBuffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
