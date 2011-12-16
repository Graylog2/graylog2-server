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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * GELFTestHelper.java: Sep 17, 2010 8:22:01 PM
 *
 * [description]
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFTestHelper {
    
    /**
     * Build a DatagramPacket containing a ZLIB compressed string
     *
     * @param content The string to compress and capsulate in the DatagramPacket
     * @return Ready DatagramPacket
     */
    public static DatagramPacket buildZLIBCompressedDatagramPacket(String content) {
        // ZLIB compress message.
        byte[] compressMe = content.getBytes();
        byte[] compressedMessage = new byte[compressMe.length];
        Deflater compressor = new Deflater();
        compressor.setInput(compressMe);
        compressor.finish();
        compressor.deflate(compressedMessage);

        // Build a datagram packet.
        return new DatagramPacket(compressedMessage, compressedMessage.length);
    }

    /**
     * Build a DatagramPacket containing a GZIP compressed string
     *
     * @param content content The string to compress and capsulate in the DatagramPacket
     * @return Ready DatagramPacket
     * @throws Exception
     */
    public static DatagramPacket buildGZIPCompressedDatagramPacket(String content) throws Exception {
        // GZIP compress message.
        ByteArrayInputStream compressMe = new ByteArrayInputStream(content.getBytes());
        ByteArrayOutputStream compressedMessage = new ByteArrayOutputStream();
        GZIPOutputStream out = new GZIPOutputStream(compressedMessage);
        for (int c = compressMe.read(); c != -1; c = compressMe.read()) {
            out.write(c);
        }
        out.close();

        return new DatagramPacket(compressedMessage.toByteArray(), compressedMessage.size());
    }
    
}
