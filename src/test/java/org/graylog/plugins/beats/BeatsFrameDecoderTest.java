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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.SucceededChannelFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.Deflater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BeatsFrameDecoderTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    @Mock
    private ChannelHandlerContext channelHandlerContext;
    @Mock
    private Channel channel;
    private BeatsFrameDecoder decoder;

    @Before
    public void setUp() throws Exception {
        decoder = new BeatsFrameDecoder();
    }

    @Test
    public void decodeWindowSizeFrame() throws Exception {
        final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeByte('2');
        buffer.writeByte('W');
        buffer.writeInt(1234);

        final Object result = decoder.decode(channelHandlerContext, channel, buffer);
        assertThat(buffer.readableBytes()).isEqualTo(0);
        assertThat(result).isNull();
        assertThat(decoder).hasFieldOrPropertyWithValue("windowSize", 1234L);
    }

    @Test
    public void decodeJsonFrame() throws Exception {
        final String json = "{\"answer\": 42}";
        final byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeByte('2');
        buffer.writeByte('J');
        // Sequence number
        buffer.writeInt(0);
        // JSON length
        buffer.writeInt(jsonBytes.length);
        // JSON
        buffer.writeBytes(jsonBytes);

        when(channel.write(anyObject())).thenReturn(new SucceededChannelFuture(channel));
        final ArgumentCaptor<ChannelBuffer> argument = ArgumentCaptor.forClass(ChannelBuffer.class);
        final ChannelBuffer[] resultBuffers = (ChannelBuffer[]) decoder.decode(channelHandlerContext, channel, buffer);
        assertThat(buffer.readableBytes()).isEqualTo(0);
        assertThat(resultBuffers).hasSize(1);
        final ChannelBuffer resultBuffer = resultBuffers[0];
        final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
        resultBuffer.readBytes(resultBytes);
        final Map<String, Object> result = objectMapper.readValue(resultBytes, new TypeReference<Map<String, Object>>() {
        });
        assertThat(result)
                .hasSize(1)
                .containsEntry("answer", 42);

        verify(channel).write(argument.capture());
        final ChannelBuffer replyBuffer = argument.getValue();
        assertThat(replyBuffer.readByte()).isEqualTo((byte) '2');
        assertThat(replyBuffer.readByte()).isEqualTo((byte) 'A');
        assertThat(replyBuffer.readUnsignedInt()).isEqualTo(0L);
        assertThat(replyBuffer.readableBytes()).isEqualTo(0);
    }

    @Test
    public void decodeDataFrame() throws Exception {
        final Map<String, String> data = ImmutableMap.of(
                "foo", "bar",
                "quux", "baz");

        final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeByte('2');
        buffer.writeByte('D');
        // Sequence number
        buffer.writeInt(0);
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

        when(channel.write(anyObject())).thenReturn(new SucceededChannelFuture(channel));
        final ArgumentCaptor<ChannelBuffer> argument = ArgumentCaptor.forClass(ChannelBuffer.class);
        final ChannelBuffer[] resultBuffers = (ChannelBuffer[]) decoder.decode(channelHandlerContext, channel, buffer);
        assertThat(buffer.readableBytes()).isEqualTo(0);
        assertThat(resultBuffers).hasSize(1);
        final ChannelBuffer resultBuffer = resultBuffers[0];
        final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
        resultBuffer.readBytes(resultBytes);
        final Map<String, Object> result = objectMapper.readValue(resultBytes, new TypeReference<Map<String, Object>>() {
        });
        assertThat(result)
                .hasSize(2)
                .containsEntry("foo", "bar")
                .containsEntry("quux", "baz");

        verify(channel).write(argument.capture());
        final ChannelBuffer replyBuffer = argument.getValue();
        assertThat(replyBuffer.readByte()).isEqualTo((byte) '2');
        assertThat(replyBuffer.readByte()).isEqualTo((byte) 'A');
        assertThat(replyBuffer.readUnsignedInt()).isEqualTo(0L);
        assertThat(replyBuffer.readableBytes()).isEqualTo(0);
    }

    @Test
    public void decodeCompressedFrame() throws Exception {
        final String json = "{\"answer\": 42}";
        final byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        final ChannelBuffer innerBuffer = ChannelBuffers.dynamicBuffer();
        innerBuffer.writeByte('2');
        innerBuffer.writeByte('J');
        // Sequence number
        innerBuffer.writeInt(0);
        // JSON length
        innerBuffer.writeInt(jsonBytes.length);
        // JSON
        innerBuffer.writeBytes(jsonBytes);

        final Deflater deflater = new Deflater(3);
        deflater.setInput(innerBuffer.array());
        deflater.finish();

        final byte[] compressedPayload = new byte[1024];
        final int compressedPayloadLength = deflater.deflate(compressedPayload);
        deflater.end();

        final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeByte('2');
        buffer.writeByte('C');
        // Compressed payload length
        buffer.writeInt(compressedPayloadLength);
        // Compressed payload
        buffer.writeBytes(compressedPayload, 0, compressedPayloadLength);

        when(channel.write(anyObject())).thenReturn(new SucceededChannelFuture(channel));
        final ArgumentCaptor<ChannelBuffer> argument = ArgumentCaptor.forClass(ChannelBuffer.class);
        final ChannelBuffer[] resultBuffers = (ChannelBuffer[]) decoder.decode(channelHandlerContext, channel, buffer);
        assertThat(buffer.readableBytes()).isEqualTo(0);
        assertThat(resultBuffers).hasSize(1);
        final ChannelBuffer resultBuffer = resultBuffers[0];
        final byte[] resultBytes = new byte[resultBuffer.readableBytes()];
        resultBuffer.readBytes(resultBytes);
        final Map<String, Object> result = objectMapper.readValue(resultBytes, new TypeReference<Map<String, Object>>() {
        });
        assertThat(result)
                .hasSize(1)
                .containsEntry("answer", 42);

        verify(channel).write(argument.capture());
        final ChannelBuffer replyBuffer = argument.getValue();
        assertThat(replyBuffer.readByte()).isEqualTo((byte) '2');
        assertThat(replyBuffer.readByte()).isEqualTo((byte) 'A');
        assertThat(replyBuffer.readUnsignedInt()).isEqualTo(0L);
        assertThat(replyBuffer.readableBytes()).isEqualTo(0);
    }
}