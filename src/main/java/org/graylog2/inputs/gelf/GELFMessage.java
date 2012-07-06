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

package org.graylog2.inputs.gelf;

import java.io.IOException;
import org.graylog2.Tools;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFMessage {

    private final byte[] payload;

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
        UNCOMPRESSED( (byte) 0x1f, (byte) 0x3c );

        private static final int HEADER_SIZE = 2;

        private final byte first;
        private final byte second;

        Type(final byte first, final byte second) {
            this.first = first;
            this.second = second;
        };

        static Type determineType(final byte first, final byte second) {
            if (first == ZLIB.first && second == ZLIB.second) {
                return ZLIB;
            } else if (first == GZIP.first) { // GZIP and UNCOMPRESSED share first magic byte
                if (second == GZIP.second) {
                    return GZIP;
                } else if (second == UNCOMPRESSED.second) {
                    return UNCOMPRESSED;
                }
            } else if (first == CHUNKED.first && second == CHUNKED.second) {
                return CHUNKED;
            }
            return UNSUPPORTED;
        }
    }

    /**
     *
     * @param payload Compressed or uncompressed (See HEADER_* constants)
     */
    public GELFMessage(final byte[] payload) {
        this.payload = payload;
    }

    public Type getGELFType() {
        if (payload.length < Type.HEADER_SIZE) {
            throw new IllegalStateException("GELF message is too short. Not even the type header would fit.");
        }
        return Type.determineType(payload[0], payload[1]);
    }

    public String getJSON(){
        try {
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
