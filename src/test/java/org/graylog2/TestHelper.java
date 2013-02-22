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

package org.graylog2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import org.graylog2.gelf.GELFMessageChunk;

public class TestHelper {

    public static byte[] zlibCompress(String what) throws UnsupportedEncodingException {
         byte[] input = what.getBytes("UTF-8");

         // Compress the bytes
         byte[] output = new byte[4096];
         Deflater compresser = new Deflater();
         compresser.setInput(input);
         compresser.finish();
         compresser.deflate(output);


        return output;
    }

    public static byte[] gzipCompress(String what) throws IOException {
        // GZIP compress message.
        ByteArrayInputStream compressMe = new ByteArrayInputStream(what.getBytes("UTF-8"));
        ByteArrayOutputStream compressedMessage = new ByteArrayOutputStream();
        GZIPOutputStream out = new GZIPOutputStream(compressedMessage);
        for (int c = compressMe.read(); c != -1; c = compressMe.read()) {
            out.write(c);
        }
        out.close();

        return compressedMessage.toByteArray();
    }

    public static byte[] buildGELFMessageChunk(long id, int seqNum, int seqCnt, byte[] data) throws Exception {

        ByteBuffer chunk=ByteBuffer.allocate(GELFMessageChunk.HEADER_TOTAL_LENGTH+data.length);
        // write header
        chunk.put((byte) 0x1e).put((byte) 0x0f).putLong(id).put((byte) seqNum).put((byte) seqCnt);
        // write body
        chunk.put(data);

        return chunk.array();
    }

    public static String toHex(String arg) throws UnsupportedEncodingException {
        return String.format("%x", new BigInteger(arg.getBytes("UTF-8")));
    }

}
