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

import java.io.IOException;
import java.net.DatagramPacket;
import org.graylog2.Main;

/**
 * GELF.java: Jun 23, 2010 6:46:45 PM
 *
 * GELF utility class
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public final class GELF {

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
     * First bytes identifying a ZLIB compressed message. (RFC 1950)
     */
    public static final String HEADER_ZLIB_COMPRESSION = "789c";

    /**
     * First bytes identifying a GZIP compressed message. (RFC 1952)
     */
    public static final String HEADER_GZIP_COMPRESSION = "1f8b";

    /**
     * First bytes identifying a chunked GELF message.
     */
    public static final String HEADER_TYPE_CHUNKED_GELF = "1e0f";

    /**
     * GELF header is 70 bytes long.
     */
    public static final int GELF_HEADER_LENGTH = 38;

    /**
     * The maximum size of the GELF data part
     */
    public static final int GELF_DATA_PART_MAX_LENGTH = 8192-GELF_HEADER_LENGTH;

    /**
     * The standard value of the _facility field.
     * https://github.com/Graylog2/graylog2-docs/wiki/GELF
     */
    public static final String STANDARD_FACILITY_VALUE = "unknown";

    /**
     * The standard value of the _level field.
     * https://github.com/Graylog2/graylog2-docs/wiki/GELF
     */
    public static final int STANDARD_LEVEL_VALUE = 1;

    /**
     * The prefix for user defined GELF fields.
     * https://github.com/Graylog2/graylog2-docs/wiki/GELF
     */
    public static final String USER_DEFINED_FIELD_PREFIX = "_";

    private GELF() { }

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
            gelfType = GELF.getGELFType(message.getData());
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

        // Throw exception if not GZIP, ZLIB or chunked
        throw new InvalidGELFTypeException();
    }

    /**
     * Find out GELF type of given byte array.
     * @param data
     * @return GELF.TYPE_[xxx] constants
     * @throws InvalidGELFCompressionMethodException
     */
    public static int getGELFType(byte[] data) throws InvalidGELFCompressionMethodException {
        // Convert first two byte to string.
        String result = "";
        for (int i=0; i < 2; i++) {
            result = result.concat(Integer.toString((data[i] & 0xff) + 0x100, 16).substring(1));
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

    /**
     * Extract the GELF header from a chunked GELF message datagram
     *
     * @param message
     * @return
     * @throws InvalidGELFHeaderException
     */
    public static GELFHeader extractGELFHeader(DatagramPacket message) throws InvalidGELFHeaderException {
        if (message.getLength() <= GELF.GELF_HEADER_LENGTH) {
            throw new InvalidGELFHeaderException("Message too short. The GELF header might not even fit here.");
        }

        // GELF header is GELF_HEADER_LENGTH bytes long. Select these bytes.
        byte[] rawGELFHeader = new byte[GELF.GELF_HEADER_LENGTH];
        for (int i = 0; i < GELF.GELF_HEADER_LENGTH; i++) {
            rawGELFHeader[i] = message.getData()[i];
        }

        // Make sure that GELF header begins with 30,15 (<3 Boris Erdmann)
        if (rawGELFHeader[0] != 30 || rawGELFHeader[1] != 15) {
            throw new InvalidGELFHeaderException("Invalid GELF ID.");
        }

        return new GELFHeader(rawGELFHeader);
    }

    /**
     * Extract the data part of a chunked GELF message datagram
     *
     * @param message
     * @return
     * @throws InvalidGELFHeaderException
     * @throws IOException
     */
    public static byte[] extractData(DatagramPacket message) throws InvalidGELFHeaderException, IOException {
        if (message.getLength() <= GELF.GELF_HEADER_LENGTH) {
            throw new InvalidGELFHeaderException();
        }

        byte[] data = new byte[message.getLength()-GELF.GELF_HEADER_LENGTH];

        int j = 0;
        for (int i = GELF.GELF_HEADER_LENGTH; i < message.getLength(); i++) {
            data[j] = message.getData()[i];
            j++;
        }

        return data;
    }

}
