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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
        super(DecodingState.PROTOCOL_VERSION, true);
    }

    @Override
    protected Object decodeLast(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, DecodingState state) throws Exception {
        // ignore, because can't send ACK after frame read
        if (buffer.readable()) {
            buffer.readBytes(super.actualReadableBytes());
        }
		        
        checkpoint(DecodingState.PROTOCOL_VERSION);
        return null;
    }
	
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, DecodingState state) throws Exception {
        ChannelBuffer[] events = null;
        switch (state) {
            case PROTOCOL_VERSION:
                checkVersion(buffer);
                checkpoint(DecodingState.FRAME_TYPE);
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
                return null;
            case FRAME_WINDOW_SIZE:
                processWindowSizeFrame(buffer);
                break;
            case FRAME_DATA:
                events = parseDataFrame(channel, buffer);
                break;
            case FRAME_COMPRESSED:
                events = processCompressedFrame(channel, buffer);
                break;
            case FRAME_JSON:
                events = parseJsonFrame(channel, buffer);
                break;
            default:
                throw new Exception("Unknown decoding state: " + state);
        }

        checkpoint(DecodingState.PROTOCOL_VERSION);
        return events;
    }

    @Nullable
    private ChannelBuffer[] processUncompressedBuffer(Channel channel, ChannelBuffer buffer) throws Exception {
        checkVersion(buffer);
        byte frameType = buffer.readByte();

        ChannelBuffer[] events = null;
        switch (frameType) {
            case FRAME_WINDOW_SIZE:
                processWindowSizeFrame(buffer);
                break;
            case FRAME_DATA:
                events = parseDataFrame(channel, buffer);
                break;
            case FRAME_COMPRESSED:
                events = processCompressedFrame(channel, buffer);
                break;
            case FRAME_JSON:
                events = parseJsonFrame(channel, buffer);
                break;
            default:
                throw new Exception("Unknown frame type: " + frameType);
        }

        return events;
    }

    private void checkVersion(ChannelBuffer channelBuffer) throws Exception {
        byte version = channelBuffer.readByte();
        if (version != PROTOCOL_VERSION) {
            throw new Exception("Unknown beats protocol version: " + version);
        }
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
            channel.write(buffer);
        }
    }

    /**
     * <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#json-frame-type">'json' frame type</a>
     */
    private ChannelBuffer[] parseJsonFrame(Channel channel, ChannelBuffer channelBuffer) throws IOException {
        sequenceNum = channelBuffer.readUnsignedInt();
        LOG.trace("Received sequence number {}", sequenceNum);

        final int jsonLength = Ints.saturatedCast(channelBuffer.readUnsignedInt());

        final ChannelBuffer buffer = channelBuffer.readSlice(jsonLength);
        sendACK(channel);

        return new ChannelBuffer[]{buffer};
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#compressed-frame-type">'compressed' frame type</a>
     */
    private ChannelBuffer[] processCompressedFrame(Channel channel, ChannelBuffer channelBuffer) throws Exception {
        final long payloadLength = channelBuffer.readUnsignedInt();
        final byte[] data = new byte[(int) payloadLength];
        channelBuffer.readBytes(data);
        try (final ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
            final InputStream in = new InflaterInputStream(dataStream)) {
            final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(ByteStreams.toByteArray(in));
            return processCompressedDataFrames(channel, buffer);
        }
    }

    private ChannelBuffer[] processCompressedDataFrames(Channel channel, ChannelBuffer channelBuffer) throws Exception {
        final List<ChannelBuffer> events = new ArrayList<>();
        while (channelBuffer.readable()) {
            final ChannelBuffer[] buffers = processUncompressedBuffer(channel, channelBuffer);
            if (buffers != null) {
                Iterables.addAll(events, Arrays.asList(buffers));
            }
        }
        return events.toArray(new ChannelBuffer[0]);
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#window-size-frame-type">'window size' frame type</a>
     */
    private void processWindowSizeFrame(ChannelBuffer channelBuffer) {
        windowSize = channelBuffer.readUnsignedInt();
        LOG.trace("Changed window size to {}", windowSize);
    }

    /**
     * @see <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md#data-frame-type">'data' frame type</a>
     */
    private ChannelBuffer[] parseDataFrame(Channel channel, ChannelBuffer channelBuffer) throws IOException {
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

        final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(outputStream.toByteArray());
        sendACK(channel);

        return new ChannelBuffer[]{buffer};
    }

    private String parseDataItem(ChannelBuffer channelBuffer) {
        long length = channelBuffer.readUnsignedInt();
        final byte[] bytes = new byte[(int) length];
        channelBuffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
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
