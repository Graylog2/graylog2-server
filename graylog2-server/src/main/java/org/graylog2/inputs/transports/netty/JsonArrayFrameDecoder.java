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
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * A decoder that splits JSON arrays into individual JSON objects using Jackson streaming API.
 * <p>
 * This decoder processes incoming ByteBuf data and extracts individual JSON objects
 * from a JSON array. It uses Jackson's streaming parser for robust JSON parsing.
 * <p>
 * Example input: [{"message":"log1"},{"message":"log2"}]
 * Will produce two frames: {"message":"log1"} and {"message":"log2"}
 */
public class JsonArrayFrameDecoder extends ByteToMessageDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(JsonArrayFrameDecoder.class);
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    /**
     * Maximum length of a single JSON object we're willing to decode.
     */
    private final int maxObjectLength;

    /**
     * True if we're currently inside a JSON array.
     */
    private boolean insideArray = false;

    /**
     * Creates a new decoder.
     *
     * @param maxObjectLength the maximum length of a single JSON object.
     *                        A {@link TooLongFrameException} is thrown if
     *                        the length of a JSON object exceeds this value.
     */
    public JsonArrayFrameDecoder(final int maxObjectLength) {
        if (maxObjectLength <= 0) {
            throw new IllegalArgumentException("maxObjectLength must be a positive integer: " + maxObjectLength);
        }
        this.maxObjectLength = maxObjectLength;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    /**
     * Create a frame out of the {@link ByteBuf} and return it.
     *
     * @param ctx    the {@link ChannelHandlerContext} which this
     *               {@link ByteToMessageDecoder} belongs to
     * @param buffer the {@link ByteBuf} from which to read data
     * @return frame the {@link ByteBuf} which represent the frame or {@code null}
     * if no frame could be created.
     */
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        if (buffer.readableBytes() == 0) {
            return null;
        }

        // Skip leading whitespace
        skipWhitespace(buffer);

        if (buffer.readableBytes() == 0) {
            return null;
        }

        // Mark the position for potential parsing
        int startPos = buffer.readerIndex();

        // Peek at the first byte to determine what we're dealing with
        byte firstByte = buffer.getByte(startPos);

        // Handle array start
        if (!insideArray && firstByte == '[') {
            buffer.skipBytes(1);
            insideArray = true;
            return null;
        }

        // Handle array end
        if (insideArray && firstByte == ']') {
            buffer.skipBytes(1);
            insideArray = false;
            return null;
        }

        // Try to extract a JSON object (also supports standalone objects outside an array)
        if (firstByte == '{') {
            return extractJsonObject(ctx, buffer);
        }

        // Unknown character, skip it
        buffer.skipBytes(1);
        return null;
    }

    /**
     * Extract a complete JSON object from the buffer.
     */
    private ByteBuf extractJsonObject(ChannelHandlerContext ctx, ByteBuf buffer) {
        final int startPos = buffer.readerIndex();

        try (ByteBufInputStream in = new ByteBufInputStream(buffer);
             JsonParser parser = JSON_FACTORY.createParser((InputStream) in)) {

            final JsonToken token = parser.nextToken();
            if (token != JsonToken.START_OBJECT) {
                buffer.readerIndex(startPos);
                return null;
            }

            // Track depth to handle nested objects
            int depth = 1;
            while (depth > 0 && parser.nextToken() != null) {
                final JsonToken t = parser.currentToken();
                if (t == JsonToken.START_OBJECT || t == JsonToken.START_ARRAY) {
                    depth++;
                } else if (t == JsonToken.END_OBJECT || t == JsonToken.END_ARRAY) {
                    depth--;
                }
            }

            if (depth != 0) {
                // Incomplete JSON object, need more data
                buffer.readerIndex(startPos);
                return null;
            }

            final int objectLength = (int) parser.currentLocation().getByteOffset();

            // Check max length. Advance past the oversized object and fire the exception
            // non-fatally so the decoder continues processing subsequent objects.
            if (objectLength > maxObjectLength) {
                buffer.readerIndex(startPos + objectLength);
                ctx.fireExceptionCaught(new TooLongFrameException(
                        "JSON object length (" + objectLength + ") exceeds the allowed maximum (" + maxObjectLength + ")"));
                return null;
            }

            // Extract the complete JSON object
            buffer.readerIndex(startPos);
            return buffer.readRetainedSlice(objectLength);

        } catch (Exception e) {
            buffer.readerIndex(startPos);

            // Jackson failed to parse. Determine if the data is malformed (complete but
            // invalid JSON) or merely incomplete (needs more bytes). Scan for a matching
            // '}' using brace depth counting on raw bytes. If found, the object boundary
            // is present but the content is invalid — skip past it and continue.
            final int malformedEnd = findClosingBrace(buffer, startPos);
            if (malformedEnd > 0) {
                buffer.readerIndex(startPos + malformedEnd);
                LOG.warn("Skipping malformed JSON object ({} bytes) at buffer position {}",
                        malformedEnd, startPos);
                ctx.fireExceptionCaught(new DecoderException(
                        "Malformed JSON object at buffer position " + startPos, e));
                return null;
            }

            // No matching '}' found — data is likely incomplete, wait for more bytes
            return null;
        }
    }

    @Override
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        super.decodeLast(ctx, in, out);
        // Reset state so a malformed array (missing ']') on one request
        // does not corrupt parsing of subsequent requests on the same connection.
        insideArray = false;
    }

    /**
     * Scan raw bytes from {@code startPos} looking for the matching '}' that closes
     * the opening '{'. Uses simple brace depth counting — this deliberately ignores
     * JSON string quoting because we only use it as a recovery heuristic for data
     * that Jackson already rejected.
     *
     * @return the number of bytes from startPos to just past the matching '}',
     * or -1 if no matching '}' was found.
     */
    private static int findClosingBrace(ByteBuf buffer, int startPos) {
        int depth = 0;
        final int end = buffer.writerIndex();
        for (int i = startPos; i < end; i++) {
            final byte b = buffer.getByte(i);
            if (b == '{') {
                depth++;
            } else if (b == '}') {
                depth--;
                if (depth == 0) {
                    return i - startPos + 1;
                }
            }
        }
        return -1;
    }

    /**
     * Skip whitespace characters.
     */
    private void skipWhitespace(ByteBuf buffer) {
        while (buffer.isReadable()) {
            byte b = buffer.getByte(buffer.readerIndex());
            if (b == ' ' || b == '\t' || b == '\n' || b == '\r') {
                buffer.skipBytes(1);
            } else {
                break;
            }
        }
    }
}
