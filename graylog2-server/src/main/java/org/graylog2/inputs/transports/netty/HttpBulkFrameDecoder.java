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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * Splits an HTTP request body into individual messages for inputs that have bulk receiving enabled.
 * <p>
 * Two payload shapes are supported:
 * <ul>
 *     <li>A JSON array, e.g. {@code [{"message":"log1"},{"message":"log2"}]}, which is split into its
 *     top-level elements. Elements may span multiple lines and may be nested arbitrarily.</li>
 *     <li>Anything else, which is split on newlines ({@code \n} or {@code \r\n}) as before. This
 *     covers plaintext payloads as well as newline delimited JSON.</li>
 * </ul>
 * The payload shape is decided by the first non-whitespace byte of the body.
 * <p>
 * This handler must be installed after an {@link io.netty.handler.codec.http.HttpObjectAggregator},
 * so that it always receives a complete request body in a single {@link ByteBuf}. Having the whole
 * body available means the handler needs no cumulation buffer and no cross-invocation state: a body
 * is either framed completely or rejected. It also means a trailing line without a delimiter is
 * emitted right away instead of being withheld until the connection closes.
 * <p>
 * Frames are emitted as retained slices of the request body. The body is never copied and message
 * contents are never re-parsed — for JSON arrays, Jackson is used to locate element boundaries only.
 */
public class HttpBulkFrameDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpBulkFrameDecoder.class);
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    /**
     * Maximum length of a single message we're willing to decode.
     */
    private final int maxFrameLength;

    /**
     * Creates a new decoder.
     *
     * @param maxFrameLength the maximum length of a single message. A {@link TooLongFrameException}
     *                       is raised if a message exceeds this value. The oversized message is
     *                       dropped, all other messages of the same body are still decoded.
     */
    public HttpBulkFrameDecoder(final int maxFrameLength) {
        if (maxFrameLength <= 0) {
            throw new IllegalArgumentException("maxFrameLength must be a positive integer: " + maxFrameLength);
        }
        this.maxFrameLength = maxFrameLength;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf body, List<Object> out) throws Exception {
        if (!body.isReadable()) {
            return;
        }

        final TooLongFrameException oversized = startsWithArray(body)
                ? decodeJsonArray(body, out)
                : decodeLines(body, out);

        if (oversized != null) {
            throw oversized;
        }
    }

    /**
     * Split a JSON array body into its top-level elements.
     *
     * @return the first {@link TooLongFrameException} encountered, or {@code null} if all elements
     * were within the length limit.
     */
    private TooLongFrameException decodeJsonArray(ByteBuf body, List<Object> out) throws Exception {
        final int base = body.readerIndex();
        TooLongFrameException oversized = null;

        try (InputStream in = new ByteBufInputStream(body.duplicate());
             JsonParser parser = JSON_FACTORY.createParser(in)) {

            parser.nextToken(); // START_ARRAY, guaranteed by startsWithArray()

            JsonToken token = parser.nextToken();
            while (token != null && token != JsonToken.END_ARRAY) {
                final int from = base + (int) parser.currentTokenLocation().getByteOffset();
                // Skips the whole element, no matter how deeply nested. No-op for scalar elements.
                parser.skipChildren();

                // The element ends where the next token starts — either the following element or the
                // closing bracket of the array. Token start offsets are exact, so the only thing left
                // to remove is the separator and any whitespace around it.
                token = parser.nextToken();
                final int nextStart = token == null
                        ? body.writerIndex()
                        : base + (int) parser.currentTokenLocation().getByteOffset();

                final TooLongFrameException e = addFrame(body, from, trimTrailing(body, from, nextStart), out);
                if (e != null && oversized == null) {
                    oversized = e;
                }
            }
        }
        return oversized;
    }

    /**
     * Split a body on {@code \n} or {@code \r\n}. Empty lines are skipped.
     *
     * @return the first {@link TooLongFrameException} encountered, or {@code null} if all lines were
     * within the length limit.
     */
    private TooLongFrameException decodeLines(ByteBuf body, List<Object> out) {
        final int end = body.writerIndex();
        int lineStart = body.readerIndex();
        TooLongFrameException oversized = null;

        for (int i = lineStart; i < end; i++) {
            if (body.getByte(i) != '\n') {
                continue;
            }
            final int lineEnd = (i > lineStart && body.getByte(i - 1) == '\r') ? i - 1 : i;
            final TooLongFrameException e = addFrame(body, lineStart, lineEnd, out);
            if (e != null && oversized == null) {
                oversized = e;
            }
            lineStart = i + 1;
        }

        // The body is complete, so a trailing line without a delimiter is a full message.
        if (lineStart < end) {
            final TooLongFrameException e = addFrame(body, lineStart, end, out);
            if (e != null && oversized == null) {
                oversized = e;
            }
        }
        return oversized;
    }

    /**
     * Emit the given range as a frame, unless it is empty or exceeds {@link #maxFrameLength}.
     *
     * @return a {@link TooLongFrameException} if the frame was dropped for being too long,
     * {@code null} otherwise.
     */
    private TooLongFrameException addFrame(ByteBuf body, int from, int to, List<Object> out) {
        final int length = to - from;
        if (length <= 0) {
            return null;
        }
        if (length > maxFrameLength) {
            LOG.warn("Skipping oversized message ({} bytes, maximum is {} bytes)", length, maxFrameLength);
            return new TooLongFrameException("Message length (" + length
                    + ") exceeds the allowed maximum (" + maxFrameLength + ")");
        }
        out.add(body.retainedSlice(from, length));
        return null;
    }

    /**
     * @return {@code true} if the first non-whitespace byte of the payload starts a JSON array.
     */
    private static boolean startsWithArray(ByteBuf body) {
        for (int i = body.readerIndex(), end = body.writerIndex(); i < end; i++) {
            final byte b = body.getByte(i);
            if (!isWhitespace(b)) {
                return b == '[';
            }
        }
        return false;
    }

    /**
     * Jackson reports the current location after a scalar element has been read, which may already
     * include the element separator. Move the end of the frame back over any trailing whitespace and
     * separators so that the emitted slice contains the element only.
     */
    private static int trimTrailing(ByteBuf body, int from, int to) {
        int end = to;
        while (end > from) {
            final byte b = body.getByte(end - 1);
            if (isWhitespace(b) || b == ',') {
                end--;
            } else {
                break;
            }
        }
        return end;
    }

    private static boolean isWhitespace(byte b) {
        return b == ' ' || b == '\t' || b == '\n' || b == '\r';
    }
}
