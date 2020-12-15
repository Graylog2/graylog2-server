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
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.TooLongFrameException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SyslogTCPFramingRouterHandlerTest {
    private EmbeddedChannel channel;

    @Before
    public void setUp() {
        channel = new EmbeddedChannel(new SyslogTCPFramingRouterHandler(32, Delimiters.lineDelimiter()));
    }

    @After
    public void tearDown() {
        assertThat(channel.finish()).isFalse();
    }

    @Test
    public void testMessageReceivedOctetFrame() {
        final ByteBuf buf = Unpooled.copiedBuffer("12 <45>Test 123", StandardCharsets.US_ASCII);
        final ByteBuf expectedBuffer = Unpooled.copiedBuffer("<45>Test 123", StandardCharsets.UTF_8);
        assertThat(channel.writeInbound(buf)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(expectedBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    public void testMessageReceivedOctetFrameMultipart() {
        final ByteBuf buf1 = Unpooled.copiedBuffer("12 <45>", StandardCharsets.US_ASCII);
        final ByteBuf buf2 = Unpooled.copiedBuffer("Test 123", StandardCharsets.US_ASCII);
        final ByteBuf expectedBuffer = Unpooled.copiedBuffer("<45>Test 123", StandardCharsets.UTF_8);
        assertThat(channel.writeInbound(buf1)).isFalse();
        assertThat(channel.writeInbound(buf2)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(expectedBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    public void testMessageReceivedOctetFrameIncomplete() {
        final ByteBuf buf = Unpooled.copiedBuffer("12 <45>", StandardCharsets.US_ASCII);
        assertThat(channel.writeInbound(buf)).isFalse();
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    public void testMessageReceivedDelimiterFrame() {
        final ByteBuf buf = Unpooled.copiedBuffer("<45>\n", StandardCharsets.US_ASCII);
        final ByteBuf expectedBuffer = Unpooled.copiedBuffer("<45>", StandardCharsets.US_ASCII);
        assertThat(channel.writeInbound(buf)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(expectedBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    public void testMessageReceivedDelimiterFrameMultipart() {
        final ByteBuf buf1 = Unpooled.copiedBuffer("<45>", StandardCharsets.US_ASCII);
        final ByteBuf buf2 = Unpooled.copiedBuffer("Test 123\n", StandardCharsets.US_ASCII);
        final ByteBuf expectedBuffer = Unpooled.copiedBuffer("<45>Test 123", StandardCharsets.US_ASCII);
        assertThat(channel.writeInbound(buf1)).isFalse();
        assertThat(channel.writeInbound(buf2)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(expectedBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    public void testMessageReceivedDelimiterFrameIncomplete() {
        final ByteBuf buf = Unpooled.copiedBuffer("<45>", StandardCharsets.US_ASCII);
        assertThat(channel.writeInbound(buf)).isFalse();
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }

    @Test
    public void testMessageReceivedDelimiterFrameLongerThanMaxFrameLength() {
        final ByteBuf buf = Unpooled.copiedBuffer("<45>012345678901234567890123456789\n", StandardCharsets.US_ASCII);
        assertThat(buf.readableBytes()).isGreaterThan(32);

        assertThatExceptionOfType(TooLongFrameException.class)
                .isThrownBy(() -> channel.writeInbound(buf))
                .withMessage("frame length (34) exceeds the allowed maximum (32)");
    }

    @Test
    public void testMessageReceivedWithEmptyBuffer() {
        final ByteBuf emptyBuffer = Unpooled.EMPTY_BUFFER;
        assertThat(channel.writeInbound(emptyBuffer)).isTrue();
        assertThat((ByteBuf) channel.readInbound()).isEqualTo(emptyBuffer);
        assertThat((ByteBuf) channel.readInbound()).isNull();
    }
}
