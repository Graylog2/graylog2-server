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

import java.util.ArrayList;
import java.util.List;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpBulkFrameDecoderTest {

    /**
     * Writes a single complete request body — which is what the handler always sees in the real
     * pipeline, because {@code HttpObjectAggregator} sits in front of it — and returns the decoded
     * frames as strings.
     */
    private static List<String> decode(String body) {
        return decode(new HttpBulkFrameDecoder(8192), body);
    }

    private static List<String> decode(HttpBulkFrameDecoder decoder, String body) {
        final EmbeddedChannel ch = new EmbeddedChannel(decoder);
        try {
            ch.writeInbound(copiedBuffer(body, CharsetUtil.UTF_8));
            return readFrames(ch);
        } finally {
            ch.finishAndReleaseAll();
        }
    }

    private static List<String> readFrames(EmbeddedChannel ch) {
        final List<String> frames = new ArrayList<>();
        ByteBuf buf;
        while ((buf = ch.readInbound()) != null) {
            frames.add(buf.toString(CharsetUtil.UTF_8));
            buf.release();
        }
        return frames;
    }

    @Test
    void simpleJsonArray() {
        assertThat(decode("[{\"message\":\"log1\"},{\"message\":\"log2\"}]"))
                .containsExactly("{\"message\":\"log1\"}", "{\"message\":\"log2\"}");
    }

    @Test
    void jsonArrayWithWhitespace() {
        assertThat(decode("[ {\"message\":\"log1\"} , {\"message\":\"log2\"} ]"))
                .containsExactly("{\"message\":\"log1\"}", "{\"message\":\"log2\"}");
    }

    @Test
    void prettyPrintedJsonArrayKeepsElementsIntact() {
        // Elements spanning multiple lines must not be split on their own newlines.
        final String body = """
                [
                  {
                    "message": "log1",
                    "level": "info"
                  },
                  {
                    "message": "log2"
                  }
                ]""";

        assertThat(decode(body)).containsExactly("""
                {
                    "message": "log1",
                    "level": "info"
                  }""", """
                {
                    "message": "log2"
                  }""");
    }

    @Test
    void nestedStructures() {
        assertThat(decode("[{\"nested\":{\"key\":\"value\"}},{\"array\":[1,2,3]},[{\"deep\":true}]]"))
                .containsExactly("{\"nested\":{\"key\":\"value\"}}", "{\"array\":[1,2,3]}", "[{\"deep\":true}]");
    }

    @Test
    void scalarArrayElements() {
        assertThat(decode("[\"text\", 42, 3.5, true, null]"))
                .containsExactly("\"text\"", "42", "3.5", "true", "null");
    }

    @Test
    void bracketsAndBracesInsideStrings() {
        assertThat(decode("[{\"a\":\"[test]\"},{\"b\":\"{test}\"},{\"c\":\"}\"},{\"d\":\"],[\"}]"))
                .containsExactly("{\"a\":\"[test]\"}", "{\"b\":\"{test}\"}", "{\"c\":\"}\"}", "{\"d\":\"],[\"}");
    }

    @Test
    void escapedQuotesAndBackslashes() {
        assertThat(decode("[{\"message\":\"Hello \\\"World\\\"\"},{\"path\":\"C:\\\\Users\\\\test\"}]"))
                .containsExactly("{\"message\":\"Hello \\\"World\\\"\"}", "{\"path\":\"C:\\\\Users\\\\test\"}");
    }

    @Test
    void unicodeInJsonArray() {
        assertThat(decode("[{\"message\":\"Hello 世界\"},{\"text\":\"café\"}]"))
                .containsExactly("{\"message\":\"Hello 世界\"}", "{\"text\":\"café\"}");
    }

    @Test
    void emptyJsonArray() {
        assertThat(decode("[]")).isEmpty();
        assertThat(decode("[ ]")).isEmpty();
    }

    @Test
    void emptyBody() {
        assertThat(decode("")).isEmpty();
    }

    @Test
    void openShiftStyleLogArray() {
        final String body = "[" +
                "{\"kubernetes\":{\"namespace_name\":\"default\",\"pod_name\":\"test-pod\"}," +
                "\"message\":\"Application started\",\"level\":\"INFO\"}," +
                "{\"kubernetes\":{\"namespace_name\":\"default\",\"pod_name\":\"test-pod\"}," +
                "\"message\":\"Processing request\",\"level\":\"DEBUG\"}" +
                "]";

        assertThat(decode(body))
                .hasSize(2)
                .anySatisfy(frame -> assertThat(frame).contains("Application started"))
                .anySatisfy(frame -> assertThat(frame).contains("Processing request"));
    }

    @Test
    void singleJsonObjectIsOneMessage() {
        assertThat(decode("{\"message\":\"log1\"}")).containsExactly("{\"message\":\"log1\"}");
    }

    @Test
    void newlineDelimitedJson() {
        assertThat(decode("{\"id\":1}\n{\"id\":2}\n"))
                .containsExactly("{\"id\":1}", "{\"id\":2}");
    }

    @Test
    void plainTextIsSplitOnNewlines() {
        assertThat(decode("first line\nsecond line\nthird line\n"))
                .containsExactly("first line", "second line", "third line");
    }

    @Test
    void plainTextWithoutTrailingNewlineIsEmittedImmediately() {
        // The body is complete, so the last line must not be withheld until the connection closes.
        assertThat(decode("first line\nsecond line")).containsExactly("first line", "second line");
        assertThat(decode("single line, no delimiter")).containsExactly("single line, no delimiter");
    }

    @Test
    void carriageReturnsAreStripped() {
        assertThat(decode("first line\r\nsecond line\r\n"))
                .containsExactly("first line", "second line");
    }

    @Test
    void emptyLinesAreSkipped() {
        assertThat(decode("first\n\n\r\nsecond\n")).containsExactly("first", "second");
    }

    @Test
    void oversizedArrayElementIsDroppedButOthersAreDecoded() {
        final EmbeddedChannel ch = new EmbeddedChannel(new HttpBulkFrameDecoder(20));
        try {
            assertThatThrownBy(() -> ch.writeInbound(copiedBuffer(
                    "[{\"id\":1},{\"message\":\"this one is much too long to be accepted\"},{\"id\":2}]",
                    CharsetUtil.UTF_8)))
                    .isInstanceOf(TooLongFrameException.class);

            assertThat(readFrames(ch)).containsExactly("{\"id\":1}", "{\"id\":2}");
        } finally {
            ch.finishAndReleaseAll();
        }
    }

    @Test
    void oversizedLineIsDroppedButOthersAreDecoded() {
        final EmbeddedChannel ch = new EmbeddedChannel(new HttpBulkFrameDecoder(20));
        try {
            assertThatThrownBy(() -> ch.writeInbound(copiedBuffer(
                    "short\nthis single line is far too long to be accepted\nalso short\n",
                    CharsetUtil.UTF_8)))
                    .isInstanceOf(TooLongFrameException.class);

            assertThat(readFrames(ch)).containsExactly("short", "also short");
        } finally {
            ch.finishAndReleaseAll();
        }
    }

    @Test
    void malformedJsonArrayFailsAfterEmittingCompleteElements() {
        final EmbeddedChannel ch = new EmbeddedChannel(new HttpBulkFrameDecoder(8192));
        try {
            assertThatThrownBy(() -> ch.writeInbound(copiedBuffer("[{\"id\":1},{not json}]", CharsetUtil.UTF_8)))
                    .isInstanceOf(DecoderException.class);

            assertThat(readFrames(ch)).containsExactly("{\"id\":1}");
        } finally {
            ch.finishAndReleaseAll();
        }
    }

    @Test
    void unclosedJsonArrayFails() {
        // Cannot happen behind HttpObjectAggregator, which only forwards complete bodies.
        final EmbeddedChannel ch = new EmbeddedChannel(new HttpBulkFrameDecoder(8192));
        try {
            assertThatThrownBy(() -> ch.writeInbound(copiedBuffer("[{\"id\":1}", CharsetUtil.UTF_8)))
                    .isInstanceOf(DecoderException.class);
        } finally {
            ch.finishAndReleaseAll();
        }
    }

    @Test
    void consecutiveBodiesOnTheSameChannelAreIndependent() {
        final EmbeddedChannel ch = new EmbeddedChannel(new HttpBulkFrameDecoder(8192));
        try {
            ch.writeInbound(copiedBuffer("[{\"id\":1}]", CharsetUtil.UTF_8));
            assertThat(readFrames(ch)).containsExactly("{\"id\":1}");

            ch.writeInbound(copiedBuffer("plain text body", CharsetUtil.UTF_8));
            assertThat(readFrames(ch)).containsExactly("plain text body");

            ch.writeInbound(copiedBuffer("[{\"id\":2},{\"id\":3}]", CharsetUtil.UTF_8));
            assertThat(readFrames(ch)).containsExactly("{\"id\":2}", "{\"id\":3}");
        } finally {
            ch.finishAndReleaseAll();
        }
    }

    @Test
    void constructorRejectsNonPositiveMaxFrameLength() {
        assertThrows(IllegalArgumentException.class, () -> new HttpBulkFrameDecoder(0));
        assertThrows(IllegalArgumentException.class, () -> new HttpBulkFrameDecoder(-1));
    }
}
