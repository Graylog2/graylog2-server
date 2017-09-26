/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.transports.netty;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.embedder.CodecEmbedderException;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.util.CharsetUtil;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;
import static org.junit.Assert.*;

public class LenientLineBasedFrameDecoderTest {
    @Test
    public void testDecodeWithStrip() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(8192, true, false, false));

        assertTrue(embedder.offer(copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII)));
        assertTrue(embedder.finish());
        assertEquals("first", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertEquals("second", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertNull(embedder.poll());

    }

    @Test
    @Ignore("Doesn't work because EmbeddedChannel#isConnected() returns always true")
    public void testDecodeLenientWithStrip() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(8192, true, false, true));

        assertTrue(embedder.offer(copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII)));
        assertTrue(embedder.finish());
        assertEquals("first", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertEquals("second", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertEquals("third", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertNull(embedder.poll());

    }

    @Test
    public void testDecodeWithoutStrip() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(8192, false, false, false));

        assertTrue(embedder.offer(copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII)));
        assertTrue(embedder.finish());
        assertEquals("first\r\n", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertEquals("second\n", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertNull(embedder.poll());

    }

    @Test
    @Ignore("Doesn't work because EmbeddedChannel#isConnected() returns always true")
    public void testDecodeLenientWithoutStrip() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(8192, false, false, true));

        assertTrue(embedder.offer(copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII)));
        assertTrue(embedder.finish());
        assertEquals("first\r\n", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertEquals("second\n", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertEquals("third", embedder.poll().toString(CharsetUtil.US_ASCII));
        assertNull(embedder.poll());

    }

    @Test
    public void testTooLongLine1() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(16, false, false, false));

        try {
            embedder.offer(copiedBuffer("12345678901234567890\r\nfirst\nsecond", CharsetUtil.US_ASCII));
            fail();
        } catch (CodecEmbedderException e) {
            assertThat(e.getCause(), is(instanceOf(TooLongFrameException.class)));
        }

        embedder.offer(wrappedBuffer(new byte[1])); // A workaround that triggers decode() once again.

        assertThat(embedder.size(), is(1));
        assertTrue(embedder.finish());

        assertThat(embedder.size(), is(1));
        assertThat(embedder.poll().toString(CharsetUtil.US_ASCII), is("first\n"));
    }

    @Test
    @Ignore("Doesn't work because EmbeddedChannel#isConnected() returns always true")
    public void testLenientTooLongLine1() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(16, false, false, true));

        try {
            embedder.offer(copiedBuffer("12345678901234567890\r\nfirst\nsecond", CharsetUtil.US_ASCII));
            fail();
        } catch (CodecEmbedderException e) {
            assertThat(e.getCause(), is(instanceOf(TooLongFrameException.class)));
        }

        embedder.offer(wrappedBuffer(new byte[1])); // A workaround that triggers decode() once again.

        assertThat(embedder.size(), is(2));
        assertTrue(embedder.finish());

        assertThat(embedder.size(), is(2));
        assertThat(embedder.poll().toString(CharsetUtil.US_ASCII), is("first\n"));
        assertThat(embedder.poll().toString(CharsetUtil.US_ASCII), is("second"));
    }

    @Test
    public void testTooLongLine2() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(16, false, false, false));

        assertFalse(embedder.offer(copiedBuffer("12345678901234567", CharsetUtil.US_ASCII)));
        try {
            embedder.offer(copiedBuffer("890\r\nfirst\r\n", CharsetUtil.US_ASCII));
            fail();
        } catch (CodecEmbedderException e) {
            assertThat(e.getCause(), is(instanceOf(TooLongFrameException.class)));
        }

        embedder.offer(wrappedBuffer(new byte[1])); // A workaround that triggers decode() once again.

        assertThat(embedder.size(), is(1));
        assertTrue(embedder.finish());

        assertThat(embedder.size(), is(1));
        assertEquals("first\r\n", embedder.poll().toString(CharsetUtil.US_ASCII));
    }

    @Test
    public void testLenientTooLongLine2() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(16, false, false, true));

        assertFalse(embedder.offer(copiedBuffer("12345678901234567", CharsetUtil.US_ASCII)));
        try {
            embedder.offer(copiedBuffer("890\r\nfirst\r\n", CharsetUtil.US_ASCII));
            fail();
        } catch (CodecEmbedderException e) {
            assertThat(e.getCause(), is(instanceOf(TooLongFrameException.class)));
        }

        embedder.offer(wrappedBuffer(new byte[1])); // A workaround that triggers decode() once again.

        assertThat(embedder.size(), is(1));
        assertTrue(embedder.finish());

        assertThat(embedder.size(), is(1));
        assertEquals("first\r\n", embedder.poll().toString(CharsetUtil.US_ASCII));
    }

    @Test
    public void testTooLongLineWithFailFast() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(16, false, true, false));

        try {
            embedder.offer(copiedBuffer("12345678901234567", CharsetUtil.US_ASCII));
            fail();
        } catch (CodecEmbedderException e) {
            assertThat(e.getCause(), is(instanceOf(TooLongFrameException.class)));
        }

        assertThat(embedder.offer(copiedBuffer("890", CharsetUtil.US_ASCII)), is(false));
        assertThat(embedder.offer(copiedBuffer("123\r\nfirst\r\n", CharsetUtil.US_ASCII)), is(true));
        assertThat(embedder.size(), is(1));
        assertTrue(embedder.finish());

        assertThat(embedder.size(), is(1));
        assertEquals("first\r\n", embedder.poll().toString(CharsetUtil.US_ASCII));
    }

    @Test
    public void testLenientTooLongLineWithFailFast() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientLineBasedFrameDecoder(16, false, true, true));

        try {
            embedder.offer(copiedBuffer("12345678901234567", CharsetUtil.US_ASCII));
            fail();
        } catch (CodecEmbedderException e) {
            assertThat(e.getCause(), is(instanceOf(TooLongFrameException.class)));
        }

        assertThat(embedder.offer(copiedBuffer("890", CharsetUtil.US_ASCII)), is(false));
        assertThat(embedder.offer(copiedBuffer("123\r\nfirst\r\n", CharsetUtil.US_ASCII)), is(true));
        assertThat(embedder.size(), is(1));
        assertTrue(embedder.finish());

        assertThat(embedder.size(), is(1));
        assertEquals("first\r\n", embedder.poll().toString(CharsetUtil.US_ASCII));
    }
}