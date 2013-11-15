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

package org.graylog2.inputs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import org.graylog2.inputs.gelf.gelf.GELFMessageChunk;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;

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

    public static byte[] buildGELFMessageChunk(String id, int seqNum, int seqCnt, byte[] data) throws Exception {
        if (id.getBytes().length != 8) {
            throw new Exception("IN TEST HELPER!!! YOU WROTE WRONG YOUR TESTS! - id must consist of 8 byte. length is: " + id.getBytes().length);
        }

        byte[] chunkData = new byte[GELFMessageChunk.HEADER_TOTAL_LENGTH+data.length]; // Magic bytes/header + data
        chunkData[0] = (byte) 0x1e;
        chunkData[1] = (byte) 0x0f;

        // 2,3,4,5,6,7,8,9 id
        System.arraycopy(id.getBytes(), 0, chunkData, 2, id.getBytes().length);

        // 10 seq num
        chunkData[10] = (byte) seqNum;

        // 11 seq cnt
        chunkData[11] = (byte) seqCnt;

        // 12... data
        System.arraycopy(data, 0, chunkData, 12, data.length);

        return chunkData;
    }

    public static String toHex(String arg) throws UnsupportedEncodingException {
        return String.format("%x", new BigInteger(arg.getBytes("UTF-8")));
    }
    
    public static Message simpleLogMessage() {
        Message m = new Message("bar", "foo", new DateTime());
        
        return m;
    }

}
