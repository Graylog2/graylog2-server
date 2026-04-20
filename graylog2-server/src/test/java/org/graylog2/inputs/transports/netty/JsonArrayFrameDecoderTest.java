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
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonArrayFrameDecoderTest {

    @Test
    public void testSimpleJsonArray() {
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
    public void testJsonArrayWithWhitespace() {
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
    public void testNestedJsonObjects() {
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
    public void testJsonWithEscapedQuotes() {
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
    public void testFragmentedJsonArray() {
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
    public void testSingleJsonObject() {
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
    public void testEmptyArray() {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[]", CharsetUtil.UTF_8));
        ch.finish();

        assertNull(ch.readInbound());
    }

    @Test
    public void testArrayWithNewlines() {
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
    public void testOversizedObjectFiresExceptionAndIsSkipped() {
        // EmbeddedChannel re-throws the TooLongFrameException at the end of writeInbound.
        // The oversized object must not be emitted as a frame.
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(20));

        assertThrows(TooLongFrameException.class, () ->
                ch.writeInbound(copiedBuffer("[{\"message\":\"this is a very long message that exceeds the limit\"}]",
                        CharsetUtil.UTF_8)));

        assertNull(ch.readInbound(), "Oversized object should not be emitted as a frame");
        ch.finishAndReleaseAll();
    }

    @Test
    public void testValidObjectsAreEmittedBeforeOversized() {
        // The decode loop runs to completion before EmbeddedChannel re-throws the exception,
        // so objects decoded before the oversized one are already queued and readable.
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(20));

        assertThrows(TooLongFrameException.class, () ->
                ch.writeInbound(copiedBuffer("[{\"id\":1},{\"message\":\"this is a very long message that exceeds the limit\"}]",
                        CharsetUtil.UTF_8)));

        ByteBuf buf = ch.readInbound();
        assertNotNull(buf, "Valid object before oversized one should be emitted");
        assertEquals("{\"id\":1}", buf.toString(CharsetUtil.UTF_8));
        buf.release();

        assertNull(ch.readInbound(), "Oversized object should not be emitted as a frame");
        ch.finishAndReleaseAll();
    }

    @Test
    public void testValidObjectsAreEmittedAfterOversized() {
        // Because the buffer is advanced past the oversized object before the exception is fired,
        // the decode loop continues and subsequent valid objects are decoded and queued before
        // EmbeddedChannel re-throws the exception at the end of writeInbound.
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(10));

        assertThrows(TooLongFrameException.class, () ->
                ch.writeInbound(copiedBuffer("[{\"message\":\"exceeds limit\"},{\"id\":1}]", CharsetUtil.UTF_8)));

        ByteBuf buf = ch.readInbound();
        assertNotNull(buf, "Valid object after oversized one should be emitted");
        assertEquals("{\"id\":1}", buf.toString(CharsetUtil.UTF_8));
        buf.release();

        assertNull(ch.readInbound());
        ch.finishAndReleaseAll();
    }

    @Test
    public void testConstructorRejectsNonPositiveMaxObjectLength() {
        assertThrows(IllegalArgumentException.class, () -> new JsonArrayFrameDecoder(0));
        assertThrows(IllegalArgumentException.class, () -> new JsonArrayFrameDecoder(-1));
    }

    @Test
    public void testMultipleArrays() {
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
    public void testComplexNestedStructure() {
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
    public void testJsonObjectsWithBracketsInStrings() {
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
    public void testJsonObjectsWithBracesInStrings() {
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
    public void testOpenShiftStyleLogArray() {
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
    public void testFragmentedJsonObjectInArray() {
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
    public void testUnicodeInJson() {
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
    public void testMultipleFragments() {
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
    public void testMalformedJsonIsSkippedAndSubsequentObjectDecoded() {
        // Malformed JSON starting with '{' should be detected via brace-scan fallback
        // and skipped, allowing valid objects sent afterward to be decoded.
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // Send malformed JSON — Jackson fails, brace-scan finds matching '}',
        // decoder skips it and fires DecoderException
        assertThrows(DecoderException.class, () ->
                ch.writeInbound(copiedBuffer("{not valid json}", CharsetUtil.UTF_8)));
        assertNull(ch.readInbound());

        // Send valid JSON — should be decoded normally
        ch.writeInbound(copiedBuffer("{\"message\":\"hello\"}", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf = ch.readInbound();
        assertNotNull(buf, "Valid JSON after malformed input should still be decoded");
        assertEquals("{\"message\":\"hello\"}", buf.toString(CharsetUtil.UTF_8));
        buf.release();

        ch.finishAndReleaseAll();
    }

    @Test
    public void testBackslashEscapedBackslash() {
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("[{\"path\":\"C:\\\\Users\\\\test\"}]", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"path\":\"C:\\\\Users\\\\test\"}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        assertNull(ch.readInbound());
    }

    @Test
    public void testMalformedJsonInsideArrayDoesNotBlockSubsequentObjects() {
        // If one object in an array is malformed, valid objects after it should still be emitted.
        // The decoder fires a DecoderException for the malformed object and continues.
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        assertThrows(DecoderException.class, () ->
                ch.writeInbound(copiedBuffer("[{not json},{\"id\":1}]", CharsetUtil.UTF_8)));

        ByteBuf buf = ch.readInbound();
        assertNotNull(buf, "Valid object after malformed one in same array should be emitted");
        assertEquals("{\"id\":1}", buf.toString(CharsetUtil.UTF_8));
        buf.release();

        ch.finishAndReleaseAll();
    }

    @Test
    public void testMissingArrayCloseDoesNotCorruptNextRequest() {
        // A malformed array payload missing ']' should not leave insideArray=true,
        // which would corrupt parsing of the next HTTP request on the same connection.
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        // First request: malformed — opening '[' but no closing ']'
        ch.writeInbound(copiedBuffer("[{\"id\":1}", CharsetUtil.UTF_8));

        ByteBuf buf1 = ch.readInbound();
        assertNotNull(buf1);
        assertEquals("{\"id\":1}", buf1.toString(CharsetUtil.UTF_8));
        buf1.release();

        // Second request: a standalone object (not in an array).
        // If insideArray leaked from the previous request, the decoder would
        // misinterpret this payload.
        ch.writeInbound(copiedBuffer("{\"id\":2}", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf2 = ch.readInbound();
        assertNotNull(buf2, "Standalone object after unclosed array should be decoded");
        assertEquals("{\"id\":2}", buf2.toString(CharsetUtil.UTF_8));
        buf2.release();

        ch.finishAndReleaseAll();
    }

    @Test
    public void testPlainTextInputDoesNotStallDecoder() {
        // Non-JSON plaintext should be skipped entirely, not block the decoder.
        EmbeddedChannel ch = new EmbeddedChannel(new JsonArrayFrameDecoder(8192));

        ch.writeInbound(copiedBuffer("just plain text", CharsetUtil.UTF_8));

        // Plain text should be consumed (skipped byte by byte) without emitting frames
        assertNull(ch.readInbound());

        // A valid object sent afterward should still work
        ch.writeInbound(copiedBuffer("{\"id\":1}", CharsetUtil.UTF_8));
        ch.finish();

        ByteBuf buf = ch.readInbound();
        assertNotNull(buf, "Valid JSON after plain text should be decoded");
        assertEquals("{\"id\":1}", buf.toString(CharsetUtil.UTF_8));
        buf.release();

        ch.finishAndReleaseAll();
    }
}
