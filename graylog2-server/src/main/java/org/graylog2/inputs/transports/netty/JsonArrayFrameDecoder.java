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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;

import java.io.IOException;
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
     * Whether or not to throw an exception as soon as we exceed maxObjectLength.
     */
    private final boolean failFast;

    /**
     * True if we're currently inside a JSON array.
     */
    private boolean insideArray = false;

    /**
     * True if we're discarding input because we're already over maxObjectLength.
     */
    private boolean discarding = false;

    /**
     * Number of bytes discarded.
     */
    private int discardedBytes = 0;

    /**
     * Creates a new decoder.
     *
     * @param maxObjectLength the maximum length of a single JSON object.
     *                        A {@link TooLongFrameException} is thrown if
     *                        the length of a JSON object exceeds this value.
     *                        <p>
     *                        By default, {@code failFast} is set to {@code true}, meaning a {@link TooLongFrameException}
     *                        will be thrown as soon as the decoder notices the length of a JSON object will exceed {@code maxObjectLength}.
     */
    public JsonArrayFrameDecoder(final int maxObjectLength) {
        this(maxObjectLength, true);
    }

    /**
     * Creates a new decoder.
     *
     * @param maxObjectLength the maximum length of a single JSON object.
     *                        A {@link TooLongFrameException} is thrown if
     *                        the length of a JSON object exceeds this value.
     * @param failFast        If <tt>true</tt>, a {@link TooLongFrameException} is
     *                        thrown as soon as the decoder notices the length of
     *                        the object will exceed <tt>maxObjectLength</tt> regardless
     *                        of whether the entire object has been read.
     *                        If <tt>false</tt>, a {@link TooLongFrameException} is
     *                        thrown after the entire object that exceeds
     *                        <tt>maxObjectLength</tt> has been read.
     */
    public JsonArrayFrameDecoder(final int maxObjectLength, final boolean failFast) {
        if (maxObjectLength <= 0) {
            throw new IllegalArgumentException("maxObjectLength must be a positive integer: " + maxObjectLength);
        }
        this.maxObjectLength = maxObjectLength;
        this.failFast = failFast;
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
        if (discarding) {
            handleDiscarding(buffer);
            return null;
        }

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

        // Try to extract a JSON object
        if (firstByte == '{') {
            return extractJsonObject(ctx, buffer);
        }

        // Skip unexpected characters (like commas between array elements)
        if (firstByte == ',') {
            buffer.skipBytes(1);
            return null;
        }

        // Unknown character, skip it
        buffer.skipBytes(1);
        return null;
    }

    /**
     * Extract a complete JSON object from the buffer.
     */
    private ByteBuf extractJsonObject(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        int startPos = buffer.readerIndex();

        try {
            // Create a byte array from the readable bytes to parse
            int readableBytes = buffer.readableBytes();
            byte[] data = new byte[readableBytes];
            buffer.getBytes(startPos, data);

            try (JsonParser parser = JSON_FACTORY.createParser(data)) {
                JsonToken token = parser.nextToken();

                if (token != JsonToken.START_OBJECT) {
                    return null; // Not a JSON object
                }

                // Track depth to handle nested objects
                int depth = 1;
                while (depth > 0 && parser.nextToken() != null) {
                    token = parser.currentToken();
                    if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                        depth++;
                    } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                        depth--;
                    }
                }

                if (depth != 0) {
                    // Incomplete JSON object, need more data
                    return null;
                }

                // Get the byte offset of where we finished parsing
                long endOffset = parser.currentLocation().getByteOffset();
                int objectLength = (int) endOffset;

                // Check max length
                if (objectLength > maxObjectLength) {
                    buffer.readerIndex(startPos + objectLength);
                    fail(ctx, objectLength);
                    return null;
                }

                // Extract the complete JSON object
                ByteBuf frame = buffer.readRetainedSlice(objectLength);

                return frame;

            } catch (IOException e) {
                // Incomplete JSON, need more data
                return null;
            }

        } catch (Exception e) {
            // If we can't parse, might be incomplete data
            if (buffer.readableBytes() > maxObjectLength) {
                fail(ctx, buffer.readableBytes());
            }
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

    /**
     * Handle discarding mode when a JSON object is too long.
     */
    private void handleDiscarding(ByteBuf buffer) throws TooLongFrameException {
        int startPos = buffer.readerIndex();
        int readableBytes = buffer.readableBytes();

        try {
            byte[] data = new byte[readableBytes];
            buffer.getBytes(startPos, data);

            try (JsonParser parser = JSON_FACTORY.createParser(data)) {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    discardedBytes += readableBytes;
                    buffer.skipBytes(readableBytes);
                    return;
                }

                int depth = 0;
                if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                    depth = 1;
                }

                while (depth > 0 && parser.nextToken() != null) {
                    token = parser.currentToken();
                    if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                        depth++;
                    } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                        depth--;
                    }
                }

                if (depth == 0) {
                    // Found the end of the structure
                    long endOffset = parser.currentLocation().getByteOffset();
                    int discardLength = (int) endOffset;
                    discardedBytes += discardLength;
                    buffer.skipBytes(discardLength);

                    int totalDiscarded = discardedBytes;
                    discardedBytes = 0;
                    discarding = false;

                    if (!failFast) {
                        fail(null, totalDiscarded);
                    }
                } else {
                    // Haven't found the end yet
                    discardedBytes += readableBytes;
                    buffer.skipBytes(readableBytes);
                }

            } catch (IOException e) {
                // Can't parse, discard everything
                discardedBytes += readableBytes;
                buffer.skipBytes(readableBytes);
            }

        } catch (Exception e) {
            // On error, discard everything
            discardedBytes += readableBytes;
            buffer.skipBytes(readableBytes);
        }
    }

    /**
     * Throw a TooLongFrameException.
     */
    private void fail(ChannelHandlerContext ctx, int length) throws TooLongFrameException {
        TooLongFrameException exception = new TooLongFrameException(
                "JSON object length (" + length + ") exceeds the allowed maximum (" + maxObjectLength + ")");

        if (ctx != null) {
            ctx.fireExceptionCaught(exception);
        } else {
            throw exception;
        }

        if (failFast) {
            discarding = true;
        }
    }
}
