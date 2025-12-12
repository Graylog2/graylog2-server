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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;

import java.util.List;

/**
 * A decoder that splits JSON arrays into individual JSON objects.
 * <p>
 * This decoder processes incoming ByteBuf data and extracts individual JSON
 * objects
 * from a JSON array. It handles nested objects and arrays correctly by tracking
 * brace and bracket depth.
 * <p>
 * Example input: [{"message":"log1"},{"message":"log2"}]
 * Will produce two frames: {"message":"log1"} and {"message":"log2"}
 */
public class JsonArrayFrameDecoder extends ByteToMessageDecoder {

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
     * <p>
     * By default, {@code failFast} is set to {@code true}, meaning a {@link TooLongFrameException}
     * will be thrown as soon as the decoder notices the length of a JSON object will exceed {@code maxObjectLength}.
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
     *                        the
     *                        object will exceed <tt>maxObjectLength</tt> regardless
     *                        of
     *                        whether the entire object has been read.
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
     *         if no frame could be created.
     */
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        if (discarding) {
            handleDiscarding(buffer);
            return null;
        }

        // Find the start of the JSON array or object
        int readerIndex = buffer.readerIndex();
        int readableBytes = buffer.readableBytes();

        if (readableBytes == 0) {
            return null;
        }

        // Skip whitespace to find the first meaningful character
        int startIndex = skipWhitespace(buffer, readerIndex, readableBytes);
        if (startIndex < 0) {
            // Only whitespace found, consume it and return
            buffer.readerIndex(buffer.writerIndex());
            return null;
        }

        buffer.readerIndex(startIndex);
        byte firstByte = buffer.getByte(startIndex);

        // Check if we're starting a JSON array
        if (!insideArray && firstByte == '[') {
            insideArray = true;
            buffer.skipBytes(1); // Skip the opening bracket
            return null; // Continue to next decode call
        }

        // If we're inside an array, look for JSON objects
        if (insideArray) {
            // Skip whitespace after array opening or comma
            int objectStart = skipWhitespace(buffer, buffer.readerIndex(), buffer.readableBytes());
            if (objectStart < 0) {
                return null; // Need more data
            }

            buffer.readerIndex(objectStart);
            byte objectFirstByte = buffer.getByte(objectStart);

            // Check for array closing
            if (objectFirstByte == ']') {
                buffer.skipBytes(1); // Skip the closing bracket
                insideArray = false;
                return null;
            }

            // Find a complete JSON object
            int objectEnd = findJsonObjectEnd(buffer, objectStart);
            if (objectEnd < 0) {
                return null; // Need more data
            }

            int objectLength = objectEnd - objectStart;
            if (objectLength > maxObjectLength) {
                buffer.readerIndex(objectEnd);
                fail(ctx, objectLength);
                return null;
            }

            // Extract the JSON object
            buffer.readerIndex(objectStart);
            ByteBuf frame = buffer.readRetainedSlice(objectLength);

            // Skip comma and whitespace after the object
            skipCommaAndWhitespace(buffer);

            return frame;
        } else {
            // Not in an array, try to extract a single JSON object or handle as-is
            int objectEnd = findJsonObjectEnd(buffer, startIndex);
            if (objectEnd < 0) {
                return null; // Need more data
            }

            int objectLength = objectEnd - startIndex;
            if (objectLength > maxObjectLength) {
                buffer.readerIndex(objectEnd);
                fail(ctx, objectLength);
                return null;
            }

            buffer.readerIndex(startIndex);
            ByteBuf frame = buffer.readRetainedSlice(objectLength);
            return frame;
        }
    }

    /**
     * Skip whitespace characters and return the index of the first non-whitespace
     * character.
     *
     * @param buffer the buffer to read from
     * @param start  the starting index
     * @param length the number of bytes to examine
     * @return the index of the first non-whitespace character, or -1 if only
     *         whitespace found
     */
    private int skipWhitespace(ByteBuf buffer, int start, int length) {
        for (int i = start; i < start + length; i++) {
            byte b = buffer.getByte(i);
            if (!isWhitespace(b)) {
                return i;
            }
        }
        return -1; // Only whitespace found
    }

    /**
     * Skip comma and whitespace after extracting a JSON object.
     */
    private void skipCommaAndWhitespace(ByteBuf buffer) {
        while (buffer.isReadable()) {
            byte b = buffer.getByte(buffer.readerIndex());
            if (b == ',' || isWhitespace(b)) {
                buffer.skipBytes(1);
            } else {
                break;
            }
        }
    }

    /**
     * Check if a byte is a whitespace character.
     */
    private boolean isWhitespace(byte b) {
        return b == ' ' || b == '\t' || b == '\n' || b == '\r';
    }

    /**
     * Find the end of a JSON object or array.
     * Returns the index after the closing brace/bracket, or -1 if not found.
     */
    private int findJsonObjectEnd(ByteBuf buffer, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int maxIndex = buffer.writerIndex();

        for (int i = start; i < maxIndex; i++) {
            byte b = buffer.getByte(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (b == '\\') {
                    escaped = true;
                } else if (b == '"') {
                    inString = false;
                }
            } else {
                if (b == '"') {
                    inString = true;
                } else if (b == '{' || b == '[') {
                    depth++;
                } else if (b == '}' || b == ']') {
                    depth--;
                    if (depth == 0) {
                        return i + 1; // Return index after closing brace/bracket
                    }
                }
            }
        }

        return -1; // Not found, need more data
    }

    /**
     * Handle discarding mode when a JSON object is too long.
     */
    private void handleDiscarding(ByteBuf buffer) throws TooLongFrameException {
        int readableBytes = buffer.readableBytes();

        // Try to find the end of the current object to resume normal operation
        int readerIndex = buffer.readerIndex();
        int objectEnd = findJsonObjectEnd(buffer, readerIndex);

        if (objectEnd >= 0) {
            // Found the end, discard and resume
            int discardLength = objectEnd - readerIndex;
            discardedBytes += discardLength;
            buffer.readerIndex(objectEnd);

            int totalDiscarded = discardedBytes;
            discardedBytes = 0;
            discarding = false;

            if (!failFast) {
                fail(null, totalDiscarded);
            }
        } else {
            // Haven't found the end yet, discard everything
            discardedBytes += readableBytes;
            buffer.readerIndex(buffer.writerIndex());
        }
    }

    /**
     * Throw a TooLongFrameException.
     */
    private void fail(ChannelHandlerContext ctx, int length) throws TooLongFrameException {
        if (ctx != null) {
            ctx.fireExceptionCaught(
                    new TooLongFrameException(
                            "JSON object length (" + length + ") exceeds the allowed maximum (" + maxObjectLength
                                    + ")"));
        } else {
            throw new TooLongFrameException(
                    "JSON object length (" + length + ") exceeds the allowed maximum (" + maxObjectLength + ")");
        }

        if (failFast) {
            discarding = true;
        }
    }
}
