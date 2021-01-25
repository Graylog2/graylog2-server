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
package org.graylog2.inputs.codecs.gelf;

import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.Tools;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GELFMessage {

    private final byte[] payload;
    private final ResolvableInetSocketAddress sourceAddress;

    /**
     * @param payload Compressed or uncompressed
     * @see GELFMessage.Type
     */
    public GELFMessage(final byte[] payload) {
        this(payload, null);
    }

    public GELFMessage(final byte[] payload, ResolvableInetSocketAddress sourceAddress) {
        this.payload = payload;
        this.sourceAddress = sourceAddress;
    }

    public Type getGELFType() {
        if (payload.length < Type.HEADER_SIZE) {
            throw new IllegalStateException("GELF message is too short. Not even the type header would fit.");
        }
        return Type.determineType(payload[0], payload[1]);
    }

    /**
     * Return the JSON payload of the GELF message
     *
     * @return The extracted JSON payload of the GELF message.
     * @deprecated Use {@link #getJSON(long)}.
     */
    @Deprecated
    public String getJSON() {
        return getJSON(Long.MAX_VALUE);
    }

    /**
     * Return the JSON payload of the GELF message.
     *
     * @param maxBytes The maximum number of bytes to read from a compressed GELF payload. {@code -1} means unlimited.
     * @return The extracted JSON payload of the GELF message.
     * @see Tools#decompressGzip(byte[], long)
     * @see Tools#decompressZlib(byte[], long)
     */
    public String getJSON(long maxBytes) {
        try {
            switch (getGELFType()) {
                case ZLIB:
                    return Tools.decompressZlib(payload, maxBytes);
                case GZIP:
                    return Tools.decompressGzip(payload, maxBytes);
                case UNCOMPRESSED:
                    return new String(payload, StandardCharsets.UTF_8);
                case CHUNKED:
                case UNSUPPORTED:
                    throw new IllegalStateException("Unknown GELF type. Not supported.");
            }
        } catch (final IOException e) {
            // Note that the UnsupportedEncodingException thrown by 'new String' can never happen because UTF-8
            // is a mandatory JRE encoding which is always present. So we only need to mention the decompress exceptions here.
            throw new IllegalStateException("Failed to decompress the GELF message payload", e);
        }
        return null;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Nullable
    public ResolvableInetSocketAddress getSourceAddress() {
        return sourceAddress;
    }

    public enum Type {

        UNSUPPORTED((byte) 0x00, (byte) 0x00),

        /**
         * A ZLIB compressed message (RFC 1950)
         */
        ZLIB((byte) 0x78, (byte) 0x9c),

        /**
         * A GZIP compressed message (RFC 1952)
         */
        GZIP((byte) 0x1f, (byte) 0x8b),

        /**
         * A chunked GELF message
         */
        CHUNKED((byte) 0x1e, (byte) 0x0f),

        /**
         * An uncompressed message, the byte values are not used.
         */
        UNCOMPRESSED((byte) 0xff, (byte) 0xff);

        private static final int HEADER_SIZE = 2;

        private final byte first;
        private final byte second;

        Type(final byte first, final byte second) {
            this.first = first;
            this.second = second;
        }

        static Type determineType(final byte first, final byte second) {

            if (first == ZLIB.first()) {
                // zlib's second byte is for flags and a checksum -
                // make sure it is positive.
                int secondInt = second;
                if (secondInt < 0) {
                    secondInt += 256;
                }
                // the second byte is not constant for zlib, it
                // differs based on compression level used and whether
                // a dictionary is used.  What the RFC guarantees is
                // that "CMF and FLG, when viewed as a 16-bit unsigned
                // integer stored in MSB order (CMF*256 + FLG), is a
                // multiple of 31"
                if ((256 * first + secondInt) % 31 == 0) {
                    return ZLIB;
                } else {
                    return UNSUPPORTED;
                }
            } else if (first == GZIP.first()) {
                if (second == GZIP.second()) {
                    return GZIP;
                } else {
                    return UNSUPPORTED;
                }
            } else if (first == CHUNKED.first()) {
                if (second == CHUNKED.second()) {
                    return CHUNKED;
                } else {
                    return UNSUPPORTED;
                }
            }
            // by default assume the payload to be "raw, uncompressed" GELF, parsing will fail if it's malformed.
            return UNCOMPRESSED;
        }

        public byte first() {
            return first;
        }

        public byte second() {
            return second;
        }
    }
}
