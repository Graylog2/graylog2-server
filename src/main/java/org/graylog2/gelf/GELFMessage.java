/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2.gelf;

import org.graylog2.Tools;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFMessage {

    private final byte[] payload;
    private final boolean raw;

    public static final String ADDITIONAL_FIELD_PREFIX = "_";

    public enum Type {

        UNSUPPORTED( (byte) 0x00, (byte) 0x00),

        /**
         * A ZLIB compressed message (RFC 1950)
         */
        ZLIB( (byte) 0x78, (byte) 0x9c ),

        /**
         * A GZIP compressed message (RFC 1952)
         */
        GZIP( (byte) 0x1f, (byte) 0x8b ),

        /**
         * A chunked GELF message
         */
        CHUNKED( (byte) 0x1e, (byte) 0x0f ),

        /**
         * An uncompressed message
         */
        UNCOMPRESSED( (byte) 0x1f, (byte) 0x3c ),

        /**
         * A raw, uncompressed message, which does not start with magic bytes.
         * Unfortunately it's tricky to find values that are not valid bytes...
         */
        RAW( (byte) 0xff, (byte) 0xff );

        private static final int HEADER_SIZE = 2;

        private final byte[] bytes;

        Type(final byte first, final byte second) {
            bytes = new byte[]{first, second};
        }

        static Type determineType(final byte first, final byte second) {

            // RAW is not handle here, because it cannot be detected by header bytes.

            if (first == ZLIB.first()) {
                // zlib's second byte is for flags and a checksum -
                // make sure it is positive.
                int secondInt = ZLIB.second();
                if (second < 0) {
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
                }
            } else if (first == GZIP.first()) { // GZIP and UNCOMPRESSED share first magic byte
                if (second == GZIP.second()) {
                    return GZIP;
                } else if (second == UNCOMPRESSED.second()) {
                    return UNCOMPRESSED;
                }
            } else if (first == CHUNKED.first() && second == CHUNKED.second()) {
                return CHUNKED;
            }
            return UNSUPPORTED;
        }

        public byte first() {
            return bytes[0];
        }

        public byte second() {
            return bytes[1];
        }

        public byte[] getBytes() {
            return bytes;
        }
    }

    /**
     *
     * @param payload Compressed or uncompressed
     * @see GELFMessage.Type
     */
    public GELFMessage(final byte[] payload) {
        this(payload, false);
    }

    /**
     *
     * @param payload Compressed or uncompressed message content
     * @param raw if <code>false</code> the payload MUST NOT start with any header bytes, it is interpreted as raw UTF-8 GELF
     */
    public GELFMessage(final byte[] payload, boolean raw) {
        this.payload = payload;
        this.raw = raw;
    }

    public Type getGELFType() {
        if (raw) {
            return Type.RAW;
        }
        if (payload.length < Type.HEADER_SIZE) {
            throw new IllegalStateException("GELF message is too short. Not even the type header would fit.");
        }
        return Type.determineType(payload[0], payload[1]);
    }

    public String getJSON(){
        try {
            if (raw) {
                return new String(payload, "UTF-8");
            }
            switch(getGELFType()) {
                case ZLIB:
                    return Tools.decompressZlib(payload);
                case GZIP:
                    return Tools.decompressGzip(payload);
                case UNCOMPRESSED:
                    // Slice off header and return pure uncompressed bytes.
                    final byte[] result = new byte[payload.length-2];
                    System.arraycopy(payload, 2, result, 0, payload.length-2);
                    return new String(result, "UTF-8");
                case CHUNKED:
                case UNSUPPORTED:
                    throw new IllegalStateException("Unknown GELF type. Not supported.");
            }
        }
        catch (final IOException e) {
            // Note that the UnsupportedEncodingException thrown by 'new String' can never happen because UTF-8
            // is a mandatory JRE encoding which is always present. So we only need to mention the decompress exceptions here.
            throw new IllegalStateException("Failed to decompress the GELF message payload", e);
        }
        return null;
    }

    public byte[] getPayload() {
        return payload;
    }
}
