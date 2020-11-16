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
package org.graylog2.inputs.transports.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LenientDelimiterBasedFrameDecoderTest {
    @Test
    public void testMultipleLinesStrippedDelimiters() {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientDelimiterBasedFrameDecoder(8192, true,
                Delimiters.lineDelimiter()));
        ch.writeInbound(Unpooled.copiedBuffer("TestLine\r\ng\r\n", Charset.defaultCharset()));

        ByteBuf buf = ch.readInbound();
        assertEquals("TestLine", buf.toString(Charset.defaultCharset()));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("g", buf2.toString(Charset.defaultCharset()));
        assertNull(ch.readInbound());
        ch.finish();

        buf.release();
        buf2.release();
    }

    @Test
    public void testIncompleteLinesStrippedDelimiters() {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientDelimiterBasedFrameDecoder(8192, true,
                Delimiters.lineDelimiter()));
        ch.writeInbound(Unpooled.copiedBuffer("Test", Charset.defaultCharset()));
        assertNull(ch.readInbound());
        ch.writeInbound(Unpooled.copiedBuffer("Line\r\ng\r\n", Charset.defaultCharset()));

        ByteBuf buf = ch.readInbound();
        assertEquals("TestLine", buf.toString(Charset.defaultCharset()));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("g", buf2.toString(Charset.defaultCharset()));
        assertNull(ch.readInbound());
        ch.finish();

        buf.release();
        buf2.release();
    }

    @Test
    public void testMultipleLines() {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientDelimiterBasedFrameDecoder(8192, false,
                Delimiters.lineDelimiter()));
        ch.writeInbound(Unpooled.copiedBuffer("TestLine\r\ng\r\n", Charset.defaultCharset()));

        ByteBuf buf = ch.readInbound();
        assertEquals("TestLine\r\n", buf.toString(Charset.defaultCharset()));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("g\r\n", buf2.toString(Charset.defaultCharset()));
        assertNull(ch.readInbound());
        ch.finish();

        buf.release();
        buf2.release();
    }

    @Test
    public void testIncompleteLines() {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientDelimiterBasedFrameDecoder(8192, false,
                Delimiters.lineDelimiter()));
        ch.writeInbound(Unpooled.copiedBuffer("Test", Charset.defaultCharset()));
        assertNull(ch.readInbound());
        ch.writeInbound(Unpooled.copiedBuffer("Line\r\ng\r\n", Charset.defaultCharset()));

        ByteBuf buf = ch.readInbound();
        assertEquals("TestLine\r\n", buf.toString(Charset.defaultCharset()));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("g\r\n", buf2.toString(Charset.defaultCharset()));
        assertNull(ch.readInbound());
        ch.finish();

        buf.release();
        buf2.release();
    }

    @Test
    public void testDecode() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(
                new LenientDelimiterBasedFrameDecoder(8192, true, Delimiters.lineDelimiter()));

        ch.writeInbound(Unpooled.copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII));

        ByteBuf buf = ch.readInbound();
        assertEquals("first", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("second", buf2.toString(CharsetUtil.US_ASCII));
        assertNull(ch.readInbound());
        ch.finish();

        ReferenceCountUtil.release(ch.readInbound());

        buf.release();
        buf2.release();
    }

    @Test
    public void testFailSlowTooLongFrameRecovery() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(
                new LenientDelimiterBasedFrameDecoder(1, true, false, false, Delimiters.nulDelimiter()));

        for (int i = 0; i < 2; i++) {
            ch.writeInbound(Unpooled.wrappedBuffer(new byte[]{1, 2}));
            try {
                assertTrue(ch.writeInbound(Unpooled.wrappedBuffer(new byte[]{0})));
                fail(DecoderException.class.getSimpleName() + " must be raised.");
            } catch (TooLongFrameException e) {
                // Expected
            }

            ch.writeInbound(Unpooled.wrappedBuffer(new byte[]{'A', 0}));
            ByteBuf buf = ch.readInbound();
            assertEquals("A", buf.toString(CharsetUtil.ISO_8859_1));

            buf.release();
        }
    }

    @Test
    public void testFailFastTooLongFrameRecovery() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(
                new LenientDelimiterBasedFrameDecoder(1, Delimiters.nulDelimiter()));

        for (int i = 0; i < 2; i++) {
            try {
                assertTrue(ch.writeInbound(Unpooled.wrappedBuffer(new byte[]{1, 2})));
                fail(DecoderException.class.getSimpleName() + " must be raised.");
            } catch (TooLongFrameException e) {
                // Expected
            }

            ch.writeInbound(Unpooled.wrappedBuffer(new byte[]{0, 'A', 0}));
            ByteBuf buf = ch.readInbound();
            assertEquals("A", buf.toString(CharsetUtil.ISO_8859_1));

            buf.release();
        }
    }

    @Test
    public void testDecodeNulDelimiter() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(
                new LenientDelimiterBasedFrameDecoder(8192, true, Delimiters.nulDelimiter()));

        ch.writeInbound(Unpooled.copiedBuffer("first\0second\0third", CharsetUtil.US_ASCII));

        ByteBuf buf = ch.readInbound();
        assertEquals("first", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("second", buf2.toString(CharsetUtil.US_ASCII));
        assertNull(ch.readInbound());
        ch.finish();

        ReferenceCountUtil.release(ch.readInbound());

        buf.release();
        buf2.release();
    }

    @Test
    public void testDecodeAndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(
                new LenientDelimiterBasedFrameDecoder(8192, true, Delimiters.lineDelimiter()));

        ch.writeInbound(Unpooled.copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII));

        ByteBuf buf = ch.readInbound();
        assertEquals("first", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("second", buf2.toString(CharsetUtil.US_ASCII));

        // Close channel
        assertTrue(ch.finish());
        ByteBuf buf3 = ch.readInbound();
        assertEquals("third", buf3.toString(CharsetUtil.US_ASCII));

        assertNull(ch.readInbound());
        assertFalse(ch.finish());

        ReferenceCountUtil.release(ch.readInbound());

        buf.release();
        buf2.release();
        buf3.release();
    }

    @Test
    public void testDecodeNulDelimiterAndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(
                new LenientDelimiterBasedFrameDecoder(8192, true, true, true, Delimiters.nulDelimiter()));

        ch.writeInbound(Unpooled.copiedBuffer("first\0second\0third", CharsetUtil.US_ASCII));

        ByteBuf buf = ch.readInbound();
        assertEquals("first", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("second", buf2.toString(CharsetUtil.US_ASCII));

        // Close channel
        assertTrue(ch.finish());
        ByteBuf buf3 = ch.readInbound();
        assertEquals("third", buf3.toString(CharsetUtil.US_ASCII));

        assertNull(ch.readInbound());
        assertFalse(ch.finish());

        ReferenceCountUtil.release(ch.readInbound());

        buf.release();
        buf2.release();
    }
}