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

import java.util.Arrays;

/**
 * GELFUtilities.java: 12.04.2012 11:20:43
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFUtilities {

    private GELFUtilities() { /* Pure utility class */ }

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
    public static final int GELF_HEADER_TYPE_LENGTH = 2;

    public static int getGELFType(byte[] magicBytes) throws Exception {

        if (Arrays.equals(magicBytes, HEADER_ZLIB_COMPRESSION)) {
            return TYPE_ZLIB;
        }

        if (Arrays.equals(magicBytes, HEADER_GZIP_COMPRESSION)) {
            return TYPE_GZIP;
        }

        if (Arrays.equals(magicBytes, HEADER_CHUNKED_GELF)) {
            return TYPE_CHUNKED;
        }

        if (Arrays.equals(magicBytes, HEADER_UNCOMPRESSED_GELF)) {
            return TYPE_GZIP;
        }
        
        throw new Exception("Unknown GELF type.");
    }

}
