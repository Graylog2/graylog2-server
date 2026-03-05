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
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonArrayFrameDecoderTest {

    @Test
    public void testSimpleJsonArray() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"message\":\"log1\"},{\"message\":\"log2\"}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"message\":\"log2\"}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testJsonArrayWithWhitespace() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[ {\"message\":\"log1\"} , {\"message\":\"log2\"} ]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"message\":\"log2\"}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testNestedJsonObjects() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"nested\":{\"key\":\"value\"}},{\"array\":[1,2,3]}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"nested\":{\"key\":\"value\"}}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"array\":[1,2,3]}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testJsonWithEscapedQuotes() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"message\":\"Hello \\\"World\\\"\"},{\"data\":\"test\"}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"message\":\"Hello \\\"World\\\"\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"data\":\"test\"}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testFragmentedJsonArray() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // Send array opening
        ch.writeInbound(copiedBuffer("[{\"message\":", CharsetUtil.UTF_8));
        assertNull(ch.readInbound()); // No complete object yet

        // Send rest of first object
        ch.writeInbound(copiedBuffer("\"log1\"},{\"message\":", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        // Send rest of array
        ch.writeInbound(copiedBuffer("\"log2\"}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"message\":\"log2\"}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testSingleJsonObject() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // Not in an array, just a single object
        ch.writeInbound(copiedBuffer("{\"message\":\"log1\"}", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testEmptyArray() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[]", CharsetUtil.UTF_8));
        ch.finish();

        assertNull(ch.readInbound());
    }

    @Test
    public void testArrayWithNewlines() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        String input = "[\n  {\"message\":\"log1\"},\n  {\"message\":\"log2\"}\n]";
        ch.writeInbound(copiedBuffer(input, CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"message\":\"log2\"}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testTooLongJsonObjectFailFast() {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(20, true));

        boolean exceptionCaught = false;
        try {
            ch.writeInbound(copiedBuffer("[{\"message\":\"this is a very long message that exceeds the limit\"}]",
                    CharsetUtil.UTF_8));
        } catch (Exception e) {
            exceptionCaught = true;
        }

        // Try to read any output and release
        Object msg = ch.readInbound();
        while (msg != null) {
            if (msg instanceof ByteBuf) {
                ((ByteBuf) msg).release();
            }
            msg = ch.readInbound();
        }

        assertTrue(exceptionCaught, "Expected TooLongFrameException to be thrown");
        ch.finishAndReleaseAll();
    }

    @Test
    public void testTooLongJsonObjectNotFailFast() {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(20, false));

        boolean exceptionCaught = false;
        try {
            ch.writeInbound(copiedBuffer("[{\"message\":\"this is a very long message that exceeds the limit\"}]",
                    CharsetUtil.UTF_8));
        } catch (Exception e) {
            exceptionCaught = true;
        }

        // Should discard the object but continue processing
        // No output expected for the oversized object
        Object msg = ch.readInbound();
        while (msg != null) {
            if (msg instanceof ByteBuf) {
                ((ByteBuf) msg).release();
            }
            msg = ch.readInbound();
        }

        assertTrue(exceptionCaught, "Expected TooLongFrameException to be thrown");
        ch.finishAndReleaseAll();
    }

    @Test
    public void testMultipleArrays() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // First array
        ch.writeInbound(copiedBuffer("[{\"id\":1},{\"id\":2}]", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"id\":1}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"id\":2}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        // Second array
        ch.writeInbound(copiedBuffer("[{\"id\":3}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf3 = ch.readInbound();
        assertNotNull(buf3);
        assertEquals("{\"id\":3}", buf3.toString(CharsetUtil.UTF_8));
        buf3.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testComplexNestedStructure() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        String input = "[{\"user\":{\"name\":\"John\",\"roles\":[\"admin\",\"user\"]},\"timestamp\":123456}," +
                "{\"user\":{\"name\":\"Jane\",\"data\":{\"nested\":{\"deep\":\"value\"}}},\"count\":5}]";
        ch.writeInbound(copiedBuffer(input, CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"user\":{\"name\":\"John\",\"roles\":[\"admin\",\"user\"]},\"timestamp\":123456}",
                buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"user\":{\"name\":\"Jane\",\"data\":{\"nested\":{\"deep\":\"value\"}}},\"count\":5}",
                buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testJsonObjectsWithBracketsInStrings() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"message\":\"[test]\"},{\"data\":\"value\"}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"message\":\"[test]\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"data\":\"value\"}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testJsonObjectsWithBracesInStrings() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"message\":\"{test}\"},{\"data\":\"}\"}\"]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"message\":\"{test}\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"data\":\"}\"}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testOpenShiftStyleLogArray() throws Exception {
        // This test simulates the actual use case from OpenShift
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        String openShiftLog = "[" +
                "{\"kubernetes\":{\"namespace_name\":\"default\",\"pod_name\":\"test-pod\"}," +
                "\"message\":\"Application started\",\"level\":\"INFO\"}," +
                "{\"kubernetes\":{\"namespace_name\":\"default\",\"pod_name\":\"test-pod\"}," +
                "\"message\":\"Processing request\",\"level\":\"DEBUG\"}" +
                "]";

        ch.writeInbound(copiedBuffer(openShiftLog, CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertTrue(buf1.toString(CharsetUtil.UTF_8).contains("Application started"));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertTrue(buf2.toString(CharsetUtil.UTF_8).contains("Processing request"));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testFragmentedJsonObjectInArray() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // Send array and partial first object
        ch.writeInbound(copiedBuffer("[{\"user\":{\"name\":", CharsetUtil.UTF_8));
        assertNull(ch.readInbound()); // Incomplete object

        // Complete first object
        ch.writeInbound(copiedBuffer("\"John\"}}", CharsetUtil.UTF_8));
        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"user\":{\"name\":\"John\"}}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        // Send second object and close array
        ch.writeInbound(copiedBuffer(",{\"user\":{\"name\":\"Jane\"}}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertEquals("{\"user\":{\"name\":\"Jane\"}}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testUnicodeInJson() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"message\":\"Hello 世界\"},{\"text\":\"café\"}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertTrue(buf1.toString(CharsetUtil.UTF_8).contains("世界"));
        buf1.release();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2);
        assertTrue(buf2.toString(CharsetUtil.UTF_8).contains("café"));
        buf2.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testMultipleFragments() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // Send one byte at a time for the array opening
        ch.writeInbound(copiedBuffer("[", CharsetUtil.UTF_8));
        assertNull(ch.readInbound());

        ch.writeInbound(copiedBuffer("{", CharsetUtil.UTF_8));
        assertNull(ch.readInbound());

        ch.writeInbound(copiedBuffer("\"key\"", CharsetUtil.UTF_8));
        assertNull(ch.readInbound());

        ch.writeInbound(copiedBuffer(":", CharsetUtil.UTF_8));
        assertNull(ch.readInbound());

        ch.writeInbound(copiedBuffer("\"value\"", CharsetUtil.UTF_8));
        assertNull(ch.readInbound());

        ch.writeInbound(copiedBuffer("}", CharsetUtil.UTF_8));
        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"key\":\"value\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        ch.writeInbound(copiedBuffer("]", CharsetUtil.UTF_8));
        ch.finish();

        assertNull(ch.readInbound());
    }

    @Test
    public void testBackslashEscapedBackslash() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"path\":\"C:\\\\Users\\\\test\"}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"path\":\"C:\\\\Users\\\\test\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        assertNull(ch.readInbound());
    }
}
