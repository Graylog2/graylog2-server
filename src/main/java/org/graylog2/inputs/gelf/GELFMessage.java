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

import java.util.Arrays;
import org.graylog2.Tools;

/**
 * GELFMessage.java: 12.04.2012 17:26:01
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFMessage {

    private byte[] payload;

    public static final String ADDITIONAL_FIELD_PREFIX = "_";

    /**
     * A ZLIB compressed message (RFC 1950)
     */
    public static final int TYPE_ZLIB = 0;

    /**
     * A GZIP compressed message (RFC 1952)
     */
    public static final int TYPE_GZIP = 1;

    /**
     * A chunked GELF message
     */
    public static final int TYPE_CHUNKED = 2;

    /**
     * An uncompressed message
     */
    public static final int TYPE_UNCOMPRESSED = 3;

    /**
     * First bytes identifying a ZLIB compressed message. (RFC 1950)
     */
    public static final byte[] HEADER_ZLIB_COMPRESSION = { (byte) 0x78, (byte) 0x9c };

    /**
     * First bytes identifying a GZIP compressed message. (RFC 1952)
     */
    public static final byte[] HEADER_GZIP_COMPRESSION = { (byte) 0x1f, (byte) 0x8b };

    /**
     * First bytes identifying a chunked GELF message.
     */
    public static final byte[] HEADER_CHUNKED_GELF = { (byte) 0x1e, (byte) 0x0f };

    /**
     * First bytes identifying an uncompressed GELF message.
     */
    public static final byte[] HEADER_UNCOMPRESSED_GELF = { (byte) 0x1f, (byte) 0x3c };

    /**
     * GELF header type size.
     */
    public static final int HEADER_TYPE_LENGTH = 2;

    /**
     *
     * @param payload Compressed or uncompressed (See HEADER_* constants)
     */
    public GELFMessage(byte[] payload) {
        this.payload = payload;
    }

    public int getGELFType() throws Exception {

        if (Arrays.equals(getMagicBytes(), HEADER_ZLIB_COMPRESSION)) {
            return TYPE_ZLIB;
        }

        if (Arrays.equals(getMagicBytes(), HEADER_GZIP_COMPRESSION)) {
            return TYPE_GZIP;
        }

        if (Arrays.equals(getMagicBytes(), HEADER_CHUNKED_GELF)) {
            return TYPE_CHUNKED;
        }

        if (Arrays.equals(getMagicBytes(), HEADER_UNCOMPRESSED_GELF)) {
            return TYPE_GZIP;
        }

        throw new Exception("Unknown GELF type.");
    }

    public String getJSON() throws Exception {
        switch(getGELFType()) {
            case TYPE_ZLIB:
                return Tools.decompressZlib(payload);
            case TYPE_GZIP:
                return Tools.decompressGzip(payload);
            default:
                throw new UnsupportedOperationException("Unknown GELF type. Not supported.");
        }
    }

    public GELFMessageChunk asChunk() throws Exception {
        return new GELFMessageChunk(this.payload);
    }

    private byte[] getMagicBytes() throws Exception {
        if (payload.length < HEADER_TYPE_LENGTH) {
            throw new Exception("GELF message is too short. Not even the type header would fit.");
        }
        
        return new byte[] {(byte) payload[0], (byte) payload[1]};
    }
    
}
