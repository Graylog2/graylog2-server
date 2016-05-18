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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.InflaterInputStream;

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

    private long windowSize;
    private long sequenceNum;

    public BeatsFrameDecoder() {
        super(true);
    }

    @Override
    protected Object decode(ChannelHandlerContext channelHandlerContext, Channel channel, ChannelBuffer channelBuffer) throws Exception {
        final ChannelBuffer[] events = processBuffer(channel, channelBuffer);
        if (events == null) {
            return null;
        } else {
            return events;
        }
    }

    @Nullable
    private ChannelBuffer[] processBuffer(Channel channel, ChannelBuffer channelBuffer) throws IOException {
        channelBuffer.markReaderIndex();
        @SuppressWarnings("unused")
        byte version = channelBuffer.readByte();
        if (LOG.isTraceEnabled() && version != PROTOCOL_VERSION) {
            LOG.trace("Unknown beats protocol version: {}", version);
        }
        byte frameType = channelBuffer.readByte();

        ChannelBuffer[] events = null;
        switch (frameType) {
            case FRAME_WINDOW_SIZE:
                processWindowSizeFrame(channelBuffer);
                break;
            case FRAME_DATA:
                events = new ChannelBuffer[]{parseDataFrame(channelBuffer)};
                sendACK(channel);
                break;
            case FRAME_COMPRESSED:
                events = processCompressedFrame(channel, channelBuffer);
                break;
            case FRAME_JSON:
                events = new ChannelBuffer[]{parseJsonFrame(channelBuffer)};
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
    private ChannelBuffer parseJsonFrame(ChannelBuffer channelBuffer) throws IOException {
        sequenceNum = channelBuffer.readUnsignedInt();
        LOG.trace("Received sequence number {}", sequenceNum);

        final int jsonLength = Ints.saturatedCast(channelBuffer.readUnsignedInt());
        return channelBuffer.readSlice(jsonLength);
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#compressed-frame-type">'compressed' frame type</a>
     */
    private ChannelBuffer[] processCompressedFrame(Channel channel, ChannelBuffer channelBuffer) throws IOException {
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

    private ChannelBuffer[] processCompressedDataFrames(Channel channel, ChannelBuffer channelBuffer) throws IOException {
        final List<ChannelBuffer> events = new ArrayList<>();
        while (channelBuffer.readable()) {
            final ChannelBuffer[] buffers = processBuffer(channel, channelBuffer);
            if (buffers != null) {
                Collections.addAll(events, buffers);
            }
        }
        return events.toArray(new ChannelBuffer[events.size()]);
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
    private ChannelBuffer parseDataFrame(ChannelBuffer channelBuffer) throws IOException {
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
        return ChannelBuffers.wrappedBuffer(outputStream.toByteArray());
    }

    private String parseDataItem(ChannelBuffer channelBuffer) {
        long length = channelBuffer.readUnsignedInt();
        final byte[] bytes = new byte[(int) length];
        channelBuffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
