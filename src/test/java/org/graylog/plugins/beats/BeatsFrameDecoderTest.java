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
import com.google.common.collect.ImmutableMap;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.Deflater;

import static org.assertj.core.api.Assertions.assertThat;

public class BeatsFrameDecoderTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private DecoderEmbedder<Iterable<ChannelBuffer>> channel;
    private BeatsFrameDecoder decoder;

    @Before
    public void setUp() throws Exception {
        decoder = new BeatsFrameDecoder();
        channel = new DecoderEmbedder<>(decoder);
    }

    @Test
    public void decodeWindowSizeFrame() throws Exception {
        final ChannelBuffer buffer = buildWindowSizeFrame(1234);

        while (buffer.readable()) {
            channel.offer(buffer);
        }
        channel.finish();
        assertThat(buffer.readableBytes()).isEqualTo(0);

        final ChannelBuffer[] output = channel.pollAll(new ChannelBuffer[0]);
        assertThat(output).isEmpty();
        assertThat(decoder.getWindowSize()).isEqualTo(1234L);
    }

    private ChannelBuffer buildWindowSizeFrame(int windowSize) {
        final ChannelBuffer buffer = ChannelBuffers.buffer(6);
        buffer.writeByte('2');
        buffer.writeByte('W');
        buffer.writeInt(windowSize);
        return buffer;
    }

    @Test
    public void decodeJsonFrame() throws Exception {
        final String json = "{\"answer\": 42}";
        final byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        final ChannelBuffer buffer = buildJsonFrame(jsonBytes, 0);

        while (buffer.readable()) {
            channel.offer(buffer);
        }
        channel.finish();

        assertThat(buffer.readableBytes()).isEqualTo(0);

        final ChannelBuffer[] output = channel.pollAll(new ChannelBuffer[0]);
        assertThat(output).hasSize(2);

        final ChannelBuffer replyBuffer = output[0];
        assertThat(extractSequenceNumber(replyBuffer)).isEqualTo(0L);

        final ChannelBuffer resultBuffer = output[1];
        final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
        resultBuffer.readBytes(resultBytes);
        final Map<String, Object> result = objectMapper.readValue(resultBytes, new TypeReference<Map<String, Object>>() {
        });
        assertThat(result)
                .hasSize(1)
                .containsEntry("answer", 42);
    }

    private ChannelBuffer buildJsonFrame(byte[] jsonBytes, int sequenceNum) {
        final ChannelBuffer buffer = ChannelBuffers.buffer(10 + jsonBytes.length);
        buffer.writeByte('2');
        buffer.writeByte('J');
        // Sequence number
        buffer.writeInt(sequenceNum);
        // JSON length
        buffer.writeInt(jsonBytes.length);
        // JSON
        buffer.writeBytes(jsonBytes);
        return buffer;
    }

    @Test
    public void decodeDataFrame() throws Exception {
        final Map<String, String> data = ImmutableMap.of(
                "foo", "bar",
                "quux", "baz");

        final ChannelBuffer buffer = buildDataFrame(data, 0);

        while (buffer.readable()) {
            channel.offer(buffer);
        }
        channel.finish();

        assertThat(buffer.readableBytes()).isEqualTo(0);

        final ChannelBuffer[] output = channel.pollAll(new ChannelBuffer[0]);
        assertThat(output).hasSize(2);

        final ChannelBuffer replyBuffer = output[0];
        assertThat(extractSequenceNumber(replyBuffer)).isEqualTo(0L);

        final ChannelBuffer resultBuffer = output[1];
        final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
        resultBuffer.readBytes(resultBytes);
        final Map<String, Object> result = objectMapper.readValue(resultBytes, new TypeReference<Map<String, Object>>() {
        });
        assertThat(result).isEqualTo(data);
    }

    private ChannelBuffer buildDataFrame(Map<String, String> data, int sequenceNum) {
        final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeByte('2');
        buffer.writeByte('D');
        // Sequence number
        buffer.writeInt(sequenceNum);
        // Number of pairs
        buffer.writeInt(data.size());
        // Pairs
        for (Map.Entry<String, String> entry : data.entrySet()) {
            final byte[] key = entry.getKey().getBytes(StandardCharsets.UTF_8);
            final byte[] value = entry.getValue().getBytes(StandardCharsets.UTF_8);
            buffer.writeInt(key.length);
            buffer.writeBytes(key);
            buffer.writeInt(value.length);
            buffer.writeBytes(value);
        }
        return buffer;
    }

    @Test
    public void decodeCompressedFrame() throws Exception {
        final String json = "{\"answer\": 42}";
        final byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        final ChannelBuffer innerBuffer = buildJsonFrame(jsonBytes, 0);
        final ChannelBuffer buffer = buildCompressedFrame(innerBuffer.array(), 3);

        while (buffer.readable()) {
            channel.offer(buffer);
        }
        channel.finish();

        assertThat(buffer.readableBytes()).isEqualTo(0);

        final ChannelBuffer[] output = channel.pollAll(new ChannelBuffer[0]);
        assertThat(output).hasSize(2);

        final ChannelBuffer replyBuffer = output[0];
        assertThat(extractSequenceNumber(replyBuffer)).isEqualTo(0L);

        final ChannelBuffer resultBuffer = output[1];
        final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
        resultBuffer.readBytes(resultBytes);
        final Map<String, Object> result = objectMapper.readValue(resultBytes, new TypeReference<Map<String, Object>>() {
        });
        assertThat(result)
                .hasSize(1)
                .containsEntry("answer", 42);
    }

    private ChannelBuffer buildCompressedFrame(byte[] payload, int compressionLevel) {
        final Deflater deflater = new Deflater(compressionLevel);
        deflater.setInput(payload);
        deflater.finish();

        final byte[] compressedPayload = new byte[1024];
        final int compressedPayloadLength = deflater.deflate(compressedPayload);
        deflater.end();

        final ChannelBuffer buffer = ChannelBuffers.buffer(6 + compressedPayloadLength);
        buffer.writeByte('2');
        buffer.writeByte('C');
        // Compressed payload length
        buffer.writeInt(compressedPayloadLength);
        // Compressed payload
        buffer.writeBytes(compressedPayload, 0, compressedPayloadLength);
        return buffer;
    }

    @Test
    public void decodeMultipleFrames() throws Exception {
        final Map<String, String> data = ImmutableMap.of("foo", "bar");
        final ChannelBuffer buffer = ChannelBuffers.copiedBuffer(
                buildWindowSizeFrame(2),
                buildDataFrame(data, 0),
                buildDataFrame(data, 1),
                buildDataFrame(data, 2)
        );

        while (buffer.readable()) {
            channel.offer(buffer);
        }
        channel.finish();

        assertThat(buffer.readableBytes()).isEqualTo(0);

        final ChannelBuffer[] output = channel.pollAll(new ChannelBuffer[0]);
        assertThat(output).hasSize(4);
        assertThat(decoder.getWindowSize()).isEqualTo(2);
        assertThat(decoder.getSequenceNum()).isEqualTo(2L);

        final ChannelBuffer replyBuffer = output[2];
        assertThat(extractSequenceNumber(replyBuffer)).isEqualTo(2L);

        final ChannelBuffer[] received = {output[0], output[1], output[3]};
        for (ChannelBuffer resultBuffer : received) {
            final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
            resultBuffer.readBytes(resultBytes);
            final Map<String, Object> result = objectMapper.readValue(resultBytes, new TypeReference<Map<String, Object>>() {
            });
            assertThat(result).isEqualTo(data);
        }
    }

    private long extractSequenceNumber(ChannelBuffer buffer) {
        assertThat(buffer.readByte()).isEqualTo((byte) '2');
        assertThat(buffer.readByte()).isEqualTo((byte) 'A');
        final long seqNum = buffer.readUnsignedInt();
        assertThat(buffer.readableBytes()).isEqualTo(0);
        return seqNum;
    }
}