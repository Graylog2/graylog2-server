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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.CodecEmbedderException;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class LenientDelimiterBasedFrameDecoderTest {
    @Test
    public void testFailSlowTooLongFrameRecovery() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientDelimiterBasedFrameDecoder(1, Delimiters.nulDelimiter()));

        for (int i = 0; i < 2; i ++) {
            embedder.offer(ChannelBuffers.wrappedBuffer(new byte[] { 1, 2 }));
            try {
                embedder.offer(ChannelBuffers.wrappedBuffer(new byte[] { 0 }));
                Assert.fail(CodecEmbedderException.class.getSimpleName() + " must be raised.");
            } catch (CodecEmbedderException e) {
                Assert.assertTrue(e.getCause() instanceof TooLongFrameException);
                // Expected
            }

            embedder.offer(ChannelBuffers.wrappedBuffer(new byte[] { 'A', 0 }));
            ChannelBuffer buf = embedder.poll();
            Assert.assertEquals("A", buf.toString(CharsetUtil.ISO_8859_1));
        }
    }

    @Test
    public void testFailFastTooLongFrameRecovery() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientDelimiterBasedFrameDecoder(1, true, true, false, Delimiters.nulDelimiter()));

        for (int i = 0; i < 2; i ++) {
            try {
                embedder.offer(ChannelBuffers.wrappedBuffer(new byte[] { 1, 2 }));
                Assert.fail(CodecEmbedderException.class.getSimpleName() + " must be raised.");
            } catch (CodecEmbedderException e) {
                Assert.assertTrue(e.getCause() instanceof TooLongFrameException);
                // Expected
            }

            embedder.offer(ChannelBuffers.wrappedBuffer(new byte[] { 0, 'A', 0 }));
            ChannelBuffer buf = embedder.poll();
            Assert.assertEquals("A", buf.toString(CharsetUtil.ISO_8859_1));
        }
    }

    @Test
    public void testLenientFailFastTooLongFrameRecovery() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientDelimiterBasedFrameDecoder(1, true, true, true, Delimiters.nulDelimiter()));

        for (int i = 0; i < 2; i ++) {
            try {
                embedder.offer(ChannelBuffers.wrappedBuffer(new byte[] { 1, 2 }));
                Assert.fail(CodecEmbedderException.class.getSimpleName() + " must be raised.");
            } catch (CodecEmbedderException e) {
                Assert.assertTrue(e.getCause() instanceof TooLongFrameException);
                // Expected
            }

            embedder.offer(ChannelBuffers.wrappedBuffer(new byte[] { 0, 'A', 0 }));
            ChannelBuffer buf = embedder.poll();
            Assert.assertEquals("A", buf.toString(CharsetUtil.ISO_8859_1));
        }
    }

    @Test
    public void testDecodeNewlines() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientDelimiterBasedFrameDecoder(8192, true, Delimiters.lineDelimiter()));

        Assert.assertTrue(embedder.offer(ChannelBuffers.copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII)));
        Assert.assertTrue(embedder.finish());
        Assert.assertEquals("first", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertEquals("second", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertNull(embedder.poll());

    }

    @Test
    @Ignore("Doesn't work because EmbeddedChannel#isConnected() returns always true")
    public void testDecodeLenientNewlines() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientDelimiterBasedFrameDecoder(8192, true, false, true, Delimiters.lineDelimiter()));

        Assert.assertTrue(embedder.offer(ChannelBuffers.copiedBuffer("first\r\nsecond\nthird", CharsetUtil.US_ASCII)));
        Assert.assertTrue(embedder.finish());
        Assert.assertEquals("first", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertEquals("second", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertEquals("third", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertNull(embedder.poll());

    }

    @Test
    public void testDecodeNul() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientDelimiterBasedFrameDecoder(8192, true, false, false, Delimiters.nulDelimiter()));

        Assert.assertTrue(embedder.offer(ChannelBuffers.copiedBuffer("first\0second\0third", CharsetUtil.US_ASCII)));
        Assert.assertTrue(embedder.finish());
        Assert.assertEquals("first", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertEquals("second", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertNull(embedder.poll());

    }

    @Test
    @Ignore("Doesn't work because EmbeddedChannel#isConnected() returns always true")
    public void testDecodeLenientNul() throws Exception {
        DecoderEmbedder<ChannelBuffer> embedder = new DecoderEmbedder<>(
                new LenientDelimiterBasedFrameDecoder(8192, true, false, true, Delimiters.nulDelimiter()));

        Assert.assertTrue(embedder.offer(ChannelBuffers.copiedBuffer("first\0second\0third", CharsetUtil.US_ASCII)));
        Assert.assertTrue(embedder.finish());
        Assert.assertEquals("first", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertEquals("second", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertEquals("third", embedder.poll().toString(CharsetUtil.US_ASCII));
        Assert.assertNull(embedder.poll());

    }
}