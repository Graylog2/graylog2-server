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
package org.graylog2.inputs.syslog.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.TooLongFrameException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Fail.fail;

class SyslogTCPFramingRouterHandlerTest {
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmbeddedChannel(new SyslogTCPFramingRouterHandler(32, Delimiters.lineDelimiter()));
    }

    @AfterEach
    void tearDown() {
        assertThat(channel.finish()).isFalse();
    }

    @Test
    void testMessageReceivedOctetFrame() {
        final ByteBuf buf = Unpooled.copiedBuffer("12 <45>Test 123", StandardCharsets.US_ASCII);
        final ByteBuf expectedBuffer = Unpooled.copiedBuffer("<45>Test 123", StandardCharsets.UTF_8);
        assertThat(channel.writeInbound(buf)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(expectedBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    void testMessageReceivedOctetFrameMultipart() {
        final ByteBuf buf1 = Unpooled.copiedBuffer("12 <45>", StandardCharsets.US_ASCII);
        final ByteBuf buf2 = Unpooled.copiedBuffer("Test 123", StandardCharsets.US_ASCII);
        final ByteBuf expectedBuffer = Unpooled.copiedBuffer("<45>Test 123", StandardCharsets.UTF_8);
        assertThat(channel.writeInbound(buf1)).isFalse();
        assertThat(channel.writeInbound(buf2)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(expectedBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    void testMessageReceivedOctetFrameIncomplete() {
        final ByteBuf buf = Unpooled.copiedBuffer("12 <45>", StandardCharsets.US_ASCII);
        assertThat(channel.writeInbound(buf)).isFalse();
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    void testMessageReceivedDelimiterFrame() {
        final ByteBuf buf = Unpooled.copiedBuffer("<45>\n", StandardCharsets.US_ASCII);
        final ByteBuf expectedBuffer = Unpooled.copiedBuffer("<45>", StandardCharsets.US_ASCII);
        assertThat(channel.writeInbound(buf)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(expectedBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    void testMessageReceivedDelimiterFrameMultipart() {
        final ByteBuf buf1 = Unpooled.copiedBuffer("<45>", StandardCharsets.US_ASCII);
        final ByteBuf buf2 = Unpooled.copiedBuffer("Test 123\n", StandardCharsets.US_ASCII);
        final ByteBuf expectedBuffer = Unpooled.copiedBuffer("<45>Test 123", StandardCharsets.US_ASCII);
        assertThat(channel.writeInbound(buf1)).isFalse();
        assertThat(channel.writeInbound(buf2)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(expectedBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    void testMessageReceivedDelimiterFrameIncomplete() {
        final ByteBuf buf = Unpooled.copiedBuffer("<45>", StandardCharsets.US_ASCII);
        assertThat(channel.writeInbound(buf)).isFalse();
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    void testMessageReceivedDelimiterFrameLongerThanMaxFrameLength() {
        final ByteBuf buf = Unpooled.copiedBuffer("<45>012345678901234567890123456789\n", StandardCharsets.US_ASCII);
        assertThat(buf.readableBytes()).isGreaterThan(32);

        assertThatExceptionOfType(TooLongFrameException.class)
                .isThrownBy(() -> channel.writeInbound(buf))
                .withMessage("frame length (34) exceeds the allowed maximum (32)");
    }

    @Test
    void testMessageReceivedWithEmptyBuffer() {
        final ByteBuf emptyBuffer = Unpooled.EMPTY_BUFFER;
        assertThat(channel.writeInbound(emptyBuffer)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(emptyBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    void testRetainedBufferReleasedDuringException() {
        // Creates an initial reference count of 1
        final ByteBuf buf = Unpooled.copiedBuffer("2025-07-29 12:34:56 myhost myapp: This is a test syslog message\n", StandardCharsets.US_ASCII);
        assertThat(buf.refCnt()).isNotZero();

        // Trying to decode aboves msg throws an exception
        assertThatExceptionOfType(DecoderException.class)
                .isThrownBy(() -> channel.writeInbound(buf));

        // Buffer is properly released even after running into an exception
        assertThat(buf.refCnt()).isZero();
    }

    @Test
    void testNotWorkingSyslogMessage() throws IOException {
        channel = new EmbeddedChannel(new SyslogTCPFramingRouterHandler(380000, Delimiters.lineDelimiter()));
        try (final InputStream is = this.getClass().getResourceAsStream("not_working_syslog_msg_1.txt")) {
            if (is != null) {
                final byte[] bytes = is.readAllBytes();
                final String expected = new String(bytes, StandardCharsets.US_ASCII).substring(0, bytes.length - 2);
                final ByteBuf buf = Unpooled.copiedBuffer(bytes);
                assertThat(channel.writeInbound(buf)).isTrue();
                final ByteBuf result = channel.readInbound();
                final String actual = result.readCharSequence(result.maxCapacity() - 1, StandardCharsets.US_ASCII).toString();
                assertThat(actual).isEqualTo(expected);
            } else {
                fail("Cannot find file.");
            }
        }

    }
}
