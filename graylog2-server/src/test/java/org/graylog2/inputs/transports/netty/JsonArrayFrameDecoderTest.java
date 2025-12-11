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
import org.junit.jupiter.api.Test;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class JsonArrayFrameDecoderTest {

    @Test
    public void testSimpleJsonArray() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"message\":\"log1\"},{\"message\":\"log2\"}]", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"message\":\"log2\"}", buf2.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
    }

    @Test
    public void testJsonArrayWithWhitespace() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[ {\"message\":\"log1\"} , {\"message\":\"log2\"} ]", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"message\":\"log2\"}", buf2.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
    }

    @Test
    public void testNestedJsonObjects() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"nested\":{\"key\":\"value\"}},{\"array\":[1,2,3]}]", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"nested\":{\"key\":\"value\"}}", buf1.toString(CharsetUtil.UTF_8));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"array\":[1,2,3]}", buf2.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
    }

    @Test
    public void testJsonWithEscapedQuotes() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"message\":\"Hello \\\"World\\\"\"},{\"data\":\"test\"}]", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"message\":\"Hello \\\"World\\\"\"}", buf1.toString(CharsetUtil.UTF_8));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"data\":\"test\"}", buf2.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
    }

    @Test
    public void testFragmentedJsonArray() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // Send array opening
        ch.writeInbound(copiedBuffer("[{\"message\":", CharsetUtil.UTF_8));
        assertNull(ch.readInbound());

        // Send rest of first object
        ch.writeInbound(copiedBuffer("\"log1\"},{\"message\":", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));

        // Send rest of array
        ch.writeInbound(copiedBuffer("\"log2\"}]", CharsetUtil.UTF_8));
        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"message\":\"log2\"}", buf2.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
    }

    @Test
    public void testSingleJsonObject() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // Not in an array, just a single object
        ch.writeInbound(copiedBuffer("{\"message\":\"log1\"}", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
    }

    @Test
    public void testEmptyArray() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[]", CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());
    }

    @Test
    public void testArrayWithNewlines() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        String input = "[\n  {\"message\":\"log1\"},\n  {\"message\":\"log2\"}\n]";
        ch.writeInbound(copiedBuffer(input, CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"message\":\"log1\"}", buf1.toString(CharsetUtil.UTF_8));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"message\":\"log2\"}", buf2.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
    }

    @Test
    public void testTooLongJsonObject() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(20, true));

        try {
            ch.writeInbound(copiedBuffer("[{\"message\":\"this is a very long message that exceeds the limit\"}]",
                    CharsetUtil.UTF_8));
            fail("Expected TooLongFrameException");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(TooLongFrameException.class)));
        }

        ch.finishAndReleaseAll();
    }

    @Test
    public void testMultipleArrays() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // First array
        ch.writeInbound(copiedBuffer("[{\"id\":1},{\"id\":2}]", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"id\":1}", buf1.toString(CharsetUtil.UTF_8));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"id\":2}", buf2.toString(CharsetUtil.UTF_8));

        // Second array
        ch.writeInbound(copiedBuffer("[{\"id\":3}]", CharsetUtil.UTF_8));

        ByteBuf buf3 = ch.readInbound();
        assertEquals("{\"id\":3}", buf3.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
        buf3.release();
    }

    @Test
    public void testComplexNestedStructure() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        String input = "[{\"user\":{\"name\":\"John\",\"roles\":[\"admin\",\"user\"]},\"timestamp\":123456}," +
                "{\"user\":{\"name\":\"Jane\",\"data\":{\"nested\":{\"deep\":\"value\"}}},\"count\":5}]";
        ch.writeInbound(copiedBuffer(input, CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"user\":{\"name\":\"John\",\"roles\":[\"admin\",\"user\"]},\"timestamp\":123456}",
                buf1.toString(CharsetUtil.UTF_8));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"user\":{\"name\":\"Jane\",\"data\":{\"nested\":{\"deep\":\"value\"}}},\"count\":5}",
                buf2.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
    }

    @Test
    public void testJsonObjectsWithBracketsInStrings() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"message\":\"[test]\"},{\"data\":\"value\"}]", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"message\":\"[test]\"}", buf1.toString(CharsetUtil.UTF_8));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"data\":\"value\"}", buf2.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
    }

    @Test
    public void testJsonObjectsWithBracesInStrings() throws Exception {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"message\":\"{test}\"},{\"data\":\"}\"}]", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertEquals("{\"message\":\"{test}\"}", buf1.toString(CharsetUtil.UTF_8));

        ByteBuf buf2 = ch.readInbound();
        assertEquals("{\"data\":\"}\"}", buf2.toString(CharsetUtil.UTF_8));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
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

        ByteBuf buf1 = ch.readInbound();
        assertTrue(buf1.toString(CharsetUtil.UTF_8).contains("Application started"));

        ByteBuf buf2 = ch.readInbound();
        assertTrue(buf2.toString(CharsetUtil.UTF_8).contains("Processing request"));

        assertNull(ch.readInbound());
        assertFalse(ch.finishAndReleaseAll());

        buf1.release();
        buf2.release();
    }
}
