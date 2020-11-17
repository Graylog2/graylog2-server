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
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.junit.Ignore;
import org.junit.Test;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LenientLineBasedFrameDecoderTest {
    @Test
    public void testDecodeWithStrip() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, true, false, false));

        ch.writeInbound(copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII));

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
    public void testDecodeWithoutStrip() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, false, false, false));

        ch.writeInbound(copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII));

        ByteBuf buf = ch.readInbound();
        assertEquals("first\r\n", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("second\n", buf2.toString(CharsetUtil.US_ASCII));
        assertNull(ch.readInbound());
        ch.finish();
        ReferenceCountUtil.release(ch.readInbound());

        buf.release();
        buf2.release();
    }

    @Test
    public void testTooLongLine1() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(16, false, false, false));

        try {
            ch.writeInbound(copiedBuffer("12345678901234567890\r\nfirst\nsecond", CharsetUtil.US_ASCII));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(TooLongFrameException.class)));
        }

        ByteBuf buf = ch.readInbound();
        ByteBuf buf2 = copiedBuffer("first\n", CharsetUtil.US_ASCII);
        assertThat(buf, is(buf2));
        assertThat(ch.finish(), is(false));

        buf.release();
        buf2.release();
    }

    @Test
    public void testTooLongLine2() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(16, false, false, false));

        assertFalse(ch.writeInbound(copiedBuffer("12345678901234567", CharsetUtil.US_ASCII)));
        try {
            ch.writeInbound(copiedBuffer("890\r\nfirst\r\n", CharsetUtil.US_ASCII));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(TooLongFrameException.class)));
        }

        ByteBuf buf = ch.readInbound();
        ByteBuf buf2 = copiedBuffer("first\r\n", CharsetUtil.US_ASCII);
        assertThat(buf, is(buf2));
        assertThat(ch.finish(), is(false));

        buf.release();
        buf2.release();
    }

    @Test
    public void testTooLongLineWithFailFast() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(16, false, true, false));

        try {
            ch.writeInbound(copiedBuffer("12345678901234567", CharsetUtil.US_ASCII));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(TooLongFrameException.class)));
        }

        assertThat(ch.writeInbound(copiedBuffer("890", CharsetUtil.US_ASCII)), is(false));
        assertThat(ch.writeInbound(copiedBuffer("123\r\nfirst\r\n", CharsetUtil.US_ASCII)), is(true));

        ByteBuf buf = ch.readInbound();
        ByteBuf buf2 = copiedBuffer("first\r\n", CharsetUtil.US_ASCII);
        assertThat(buf, is(buf2));
        assertThat(ch.finish(), is(false));

        buf.release();
        buf2.release();
    }

    @Test
    public void testDecodeSplitsCorrectly() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, false, false, false));

        assertTrue(ch.writeInbound(copiedBuffer("line\r\n.\r\n", CharsetUtil.US_ASCII)));

        ByteBuf buf = ch.readInbound();
        assertEquals("line\r\n", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals(".\r\n", buf2.toString(CharsetUtil.US_ASCII));
        assertFalse(ch.finishAndReleaseAll());

        buf.release();
        buf2.release();
    }

    @Test
    public void testFragmentedDecode() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, false, false, false));

        assertFalse(ch.writeInbound(copiedBuffer("huu", CharsetUtil.US_ASCII)));
        assertNull(ch.readInbound());

        assertFalse(ch.writeInbound(copiedBuffer("haa\r", CharsetUtil.US_ASCII)));
        assertNull(ch.readInbound());

        assertTrue(ch.writeInbound(copiedBuffer("\nhuuhaa\r\n", CharsetUtil.US_ASCII)));
        ByteBuf buf = ch.readInbound();
        assertEquals("huuhaa\r\n", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("huuhaa\r\n", buf2.toString(CharsetUtil.US_ASCII));
        assertFalse(ch.finishAndReleaseAll());

        buf.release();
        buf2.release();
    }

    @Test
    public void testEmptyLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, true, false, false));

        assertTrue(ch.writeInbound(copiedBuffer("\nabcna\r\n", CharsetUtil.US_ASCII)));

        ByteBuf buf = ch.readInbound();
        assertEquals("", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("abcna", buf2.toString(CharsetUtil.US_ASCII));

        assertFalse(ch.finishAndReleaseAll());

        buf.release();
        buf2.release();
    }

    @Test
    public void testDecodeWithStripAndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, true, false, true));

        ch.writeInbound(copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("first", buf1.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("second", buf2.toString(CharsetUtil.US_ASCII));

        // Close channel
        assertTrue(ch.finish());

        ByteBuf buf3 = ch.readInbound();
        assertEquals("third", buf3.toString(CharsetUtil.US_ASCII));

        assertNull(ch.readInbound());
        assertFalse(ch.finish());

        ReferenceCountUtil.release(ch.readInbound());

        buf1.release();
        buf2.release();
        buf3.release();
    }

    @Test
    public void testDecodeWithoutStripAndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, false, false, true));

        ch.writeInbound(copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("first\r\n", buf1.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("second\n", buf2.toString(CharsetUtil.US_ASCII));

        // Close channel
        assertTrue(ch.finish());

        ByteBuf buf3 = ch.readInbound();
        assertEquals("third", buf3.toString(CharsetUtil.US_ASCII));

        assertNull(ch.readInbound());
        assertFalse(ch.finish());
        ReferenceCountUtil.release(ch.readInbound());

        buf1.release();
        buf2.release();
        buf3.release();
    }

    @Test
    public void testTooLongLine1AndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(16, false, false, true));

        try {
            ch.writeInbound(copiedBuffer("12345678901234567890\r\nfirst\nsecond", CharsetUtil.US_ASCII));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(TooLongFrameException.class)));
        }

        ByteBuf buf = ch.readInbound();
        ByteBuf expectedBuf = copiedBuffer("first\n", CharsetUtil.US_ASCII);
        assertThat(buf, is(expectedBuf));

        // Close channel
        assertTrue(ch.finish());

        ByteBuf buf2 = ch.readInbound();
        ByteBuf expectedBuf2 = copiedBuffer("second", CharsetUtil.US_ASCII);

        assertThat(buf2, is(expectedBuf2));
        assertThat(ch.finish(), is(false));

        buf.release();
        expectedBuf.release();
        buf2.release();
        expectedBuf2.release();
    }

    @Test
    public void testTooLongLine2AndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(16, false, false, true));

        assertFalse(ch.writeInbound(copiedBuffer("12345678901234567", CharsetUtil.US_ASCII)));
        try {
            ch.writeInbound(copiedBuffer("890\r\nfirst\r\n", CharsetUtil.US_ASCII));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(TooLongFrameException.class)));
        }

        ByteBuf buf = ch.readInbound();
        ByteBuf buf2 = copiedBuffer("first\r\n", CharsetUtil.US_ASCII);
        assertThat(buf, is(buf2));
        assertThat(ch.finish(), is(false));

        buf.release();
        buf2.release();
    }

    @Test
    public void testTooLongLineWithFailFastAndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(16, false, true, true));

        try {
            ch.writeInbound(copiedBuffer("12345678901234567", CharsetUtil.US_ASCII));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(TooLongFrameException.class)));
        }

        assertThat(ch.writeInbound(copiedBuffer("890", CharsetUtil.US_ASCII)), is(false));
        assertThat(ch.writeInbound(copiedBuffer("123\r\nfirst\r\n", CharsetUtil.US_ASCII)), is(true));

        ByteBuf buf = ch.readInbound();
        ByteBuf buf2 = copiedBuffer("first\r\n", CharsetUtil.US_ASCII);
        assertThat(buf, is(buf2));
        assertThat(ch.finish(), is(false));

        buf.release();
        buf2.release();
    }

    @Test
    public void testDecodeSplitsCorrectlyAndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, false, false, true));

        assertTrue(ch.writeInbound(copiedBuffer("line\r\n.\r\n", CharsetUtil.US_ASCII)));

        ByteBuf buf = ch.readInbound();
        assertEquals("line\r\n", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals(".\r\n", buf2.toString(CharsetUtil.US_ASCII));
        assertFalse(ch.finishAndReleaseAll());

        buf.release();
        buf2.release();
    }

    @Test
    public void testFragmentedDecodeAndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, false, false, true));

        assertFalse(ch.writeInbound(copiedBuffer("huu", CharsetUtil.US_ASCII)));
        assertNull(ch.readInbound());

        assertFalse(ch.writeInbound(copiedBuffer("haa\r", CharsetUtil.US_ASCII)));
        assertNull(ch.readInbound());

        assertTrue(ch.writeInbound(copiedBuffer("\nhuuhaa\r\n", CharsetUtil.US_ASCII)));
        ByteBuf buf = ch.readInbound();
        assertEquals("huuhaa\r\n", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("huuhaa\r\n", buf2.toString(CharsetUtil.US_ASCII));
        assertFalse(ch.finishAndReleaseAll());

        buf.release();
        buf2.release();
    }

    @Test
    public void testEmptyLineAndEmitLastLine() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new LenientLineBasedFrameDecoder(8192, true, false, true));

        assertTrue(ch.writeInbound(copiedBuffer("\nabcna\r\n", CharsetUtil.US_ASCII)));

        ByteBuf buf = ch.readInbound();
        assertEquals("", buf.toString(CharsetUtil.US_ASCII));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("abcna", buf2.toString(CharsetUtil.US_ASCII));

        assertFalse(ch.finishAndReleaseAll());

        buf.release();
        buf2.release();
    }
}