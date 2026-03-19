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
import io.netty.handler.codec.TooLongFrameException;

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
            // Incomplete or unparseable JSON — wait for more data
            buffer.readerIndex(startPos);
            return null;
        }
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
