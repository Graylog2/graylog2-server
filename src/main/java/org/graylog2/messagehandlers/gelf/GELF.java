/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.gelf;

import java.net.DatagramPacket;
import org.graylog2.Main;

/**
 * GELF.java: Jun 23, 2010 6:46:45 PM
 *
 * GELF utility class
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELF {

    public static final int TYPE_ZLIB = 0;
    public static final int TYPE_GZIP = 1;
    public static final int TYPE_CHUNKED = 2;

    /**
     * First bytes identifying a ZLIB compressed message. (RFC 1950)
     */
    public static final String HEADER_ZLIB_COMPRESSION = "789c";

    /**
     * First bytes identifying a GZIP compressed message. (RFC 1952)
     */
    public static final String HEADER_GZIP_COMPRESSION = "1f8b";

    public static final String HEADER_TYPE_CHUNKED_GELF = "3045";

    /**
     * Is GELF enabled? Decision based on /etc/graylog2.conf "use_gelf" parameter.
     * @return boolean
     */
    public static boolean isEnabled() {
        return Main.masterConfig.getProperty("use_gelf").equals("true");
    }

    /**
     * Find out if the given string is a chunked GELF message or not.
     *
     * @param message The message to inspect
     * @return boolean
     * @throws InvalidGELFTypeException
     */
    public static boolean isChunkedMessage(DatagramPacket message) throws InvalidGELFTypeException {
        // Message must be longer than 4 byte. (Needed to find out header)
        if (message.getLength() <= 4) {
            throw new InvalidGELFTypeException();
        }

        int gelfType;
        try {
            gelfType = GELF.getGELFType(message);
        } catch (InvalidGELFCompressionMethodException e) {
            throw new InvalidGELFTypeException("Unknown compression method.");
        }

        switch(gelfType) {
            case GELF.TYPE_GZIP:
            case GELF.TYPE_ZLIB:
                return false;
            case GELF.TYPE_CHUNKED:
                return true;
        }

        // THROW EXCEPTION IF NOT 1 OR 2
        throw new InvalidGELFTypeException();
    }

    public static int getGELFType(DatagramPacket message) throws InvalidGELFCompressionMethodException {
        // Convert first two byte to string.
        String result = "";
        for (int i=0; i < 2; i++) {
            result += Integer.toString((message.getData()[i] & 0xff) + 0x100, 16).substring(1);
        }

        if (result.equals(GELF.HEADER_GZIP_COMPRESSION)) {
            return GELF.TYPE_GZIP;
        }

        if (result.equals(GELF.HEADER_ZLIB_COMPRESSION)) {
            return GELF.TYPE_ZLIB;
        }

        if (result.equals(GELF.HEADER_TYPE_CHUNKED_GELF)) {
            return GELF.TYPE_CHUNKED;
        }

        throw new InvalidGELFCompressionMethodException();
    }

}
