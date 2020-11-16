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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LoggingHandler;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.Deflater;

import static org.assertj.core.api.Assertions.assertThat;

public class BeatsFrameDecoderTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private EmbeddedChannel channel;
    private BeatsFrameDecoder decoder;

    @Before
    public void setUp() throws Exception {
        decoder = new BeatsFrameDecoder();
        channel = new EmbeddedChannel(new LoggingHandler(), decoder);
    }

    @Test
    public void decodeWindowSizeFrame() throws Exception {
        final ByteBuf buffer = buildWindowSizeFrame(1234);

        while (buffer.isReadable()) {
            channel.writeInbound(buffer);
        }
        channel.finish();
        assertThat(buffer.readableBytes()).isEqualTo(0);

        assertThat((Object) channel.readInbound()).isNull();
        assertThat(decoder.getWindowSize()).isEqualTo(1234L);
    }

    private ByteBuf buildWindowSizeFrame(int windowSize) {
        final ByteBuf buffer = Unpooled.buffer(6);
        buffer.writeByte('2');
        buffer.writeByte('W');
        buffer.writeInt(windowSize);
        return buffer;
    }

    @Test
    public void decodeJsonFrame() throws Exception {
        final String json = "{\"answer\": 42}";
        final byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        final ByteBuf buffer = buildJsonFrame(jsonBytes, 0);

        while (buffer.isReadable()) {
            channel.writeInbound(buffer);
        }
        channel.finish();

        assertThat(buffer.readableBytes()).isEqualTo(0);

        final ByteBuf replyBuffer = channel.readOutbound();
        assertThat(extractSequenceNumber(replyBuffer)).isEqualTo(0L);

        final ByteBuf resultBuffer = channel.readInbound();
        final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
        resultBuffer.readBytes(resultBytes);
        final Map<String, Object> result = objectMapper.readValue(resultBytes, TypeReferences.MAP_STRING_OBJECT);
        assertThat(result)
                .hasSize(1)
                .containsEntry("answer", 42);
    }

    private ByteBuf buildJsonFrame(byte[] jsonBytes, int sequenceNum) {
        final ByteBuf buffer = Unpooled.buffer(10 + jsonBytes.length);
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

        final ByteBuf buffer = buildDataFrame(data, 0);

        while (buffer.isReadable()) {
            channel.writeInbound(buffer);
        }
        channel.finish();

        assertThat(buffer.readableBytes()).isEqualTo(0);

        final ByteBuf replyBuffer = channel.readOutbound();
        assertThat(extractSequenceNumber(replyBuffer)).isEqualTo(0L);

        final ByteBuf resultBuffer = channel.readInbound();
        final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
        resultBuffer.readBytes(resultBytes);
        final Map<String, Object> result = objectMapper.readValue(resultBytes, TypeReferences.MAP_STRING_OBJECT);
        assertThat(result).isEqualTo(data);
    }

    private ByteBuf buildDataFrame(Map<String, String> data, int sequenceNum) {
        final ByteBuf buffer = Unpooled.buffer();
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

        final ByteBuf innerBuffer = buildJsonFrame(jsonBytes, 0);
        final ByteBuf buffer = buildCompressedFrame(innerBuffer.array(), 3);

        while (buffer.isReadable()) {
            channel.writeInbound(buffer);
        }
        channel.finish();

        assertThat(buffer.readableBytes()).isEqualTo(0);

        final ByteBuf replyBuffer = channel.readOutbound();
        assertThat(extractSequenceNumber(replyBuffer)).isEqualTo(0L);

        final ByteBuf resultBuffer = channel.readInbound();
        final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
        resultBuffer.readBytes(resultBytes);
        final Map<String, Object> result = objectMapper.readValue(resultBytes, TypeReferences.MAP_STRING_OBJECT);
        assertThat(result)
                .hasSize(1)
                .containsEntry("answer", 42);
    }

    private ByteBuf buildCompressedFrame(byte[] payload, int compressionLevel) {
        final Deflater deflater = new Deflater(compressionLevel);
        deflater.setInput(payload);
        deflater.finish();

        final byte[] compressedPayload = new byte[1024];
        final int compressedPayloadLength = deflater.deflate(compressedPayload);
        deflater.end();

        final ByteBuf buffer = Unpooled.buffer(6 + compressedPayloadLength);
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
        final ByteBuf buffer = Unpooled.copiedBuffer(
                buildWindowSizeFrame(2),
                buildDataFrame(data, 0),
                buildDataFrame(data, 1),
                buildDataFrame(data, 2)
        );

        while (buffer.isReadable()) {
            channel.writeInbound(buffer);
        }
        channel.finish();

        assertThat(buffer.readableBytes()).isEqualTo(0);

        final ByteBuf output1 = channel.readInbound();
        final ByteBuf output2 = channel.readInbound();
        final ByteBuf output3 = channel.readInbound();
        assertThat(decoder.getWindowSize()).isEqualTo(2);
        assertThat(decoder.getSequenceNum()).isEqualTo(2L);

        final ByteBuf replyBuffer = channel.readOutbound();
        assertThat(extractSequenceNumber(replyBuffer)).isEqualTo(2L);

        final ByteBuf[] received = {output1, output2, output3};
        for (ByteBuf resultBuffer : received) {
            final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
            resultBuffer.readBytes(resultBytes);
            final Map<String, Object> result = objectMapper.readValue(resultBytes, TypeReferences.MAP_STRING_OBJECT);
            assertThat(result).isEqualTo(data);
        }
    }

    private long extractSequenceNumber(ByteBuf buffer) {
        assertThat(buffer.readByte()).isEqualTo((byte) '2');
        assertThat(buffer.readByte()).isEqualTo((byte) 'A');
        final long seqNum = buffer.readUnsignedInt();
        assertThat(buffer.readableBytes()).isEqualTo(0);
        return seqNum;
    }
}