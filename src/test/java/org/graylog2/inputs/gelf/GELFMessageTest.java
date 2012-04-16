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

import org.graylog2.TestHelper;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart.koopmann
 */
public class GELFMessageTest {

    public final static String GELF_JSON = "{\"message\":\"foo\",\"host\":\"bar\",\"_lol_utf8\":\"ü\"}";

    @Test
    public void testGetGELFTypeDetectsZLIBCompressedMessage() throws Exception {
        byte[] fakeData = new byte[20];
        fakeData[0] = (byte) 0x78;
        fakeData[1] = (byte) 0x9c;

        GELFMessage msg = new GELFMessage(fakeData);
        assertEquals(GELFMessage.TYPE_ZLIB, msg.getGELFType());
    }

    @Test
    public void testGetGELFTypeDetectsGZIPCompressedMessage() throws Exception {
        byte[] fakeData = new byte[20];
        fakeData[0] = (byte) 0x1f;
        fakeData[1] = (byte) 0x8b;

        GELFMessage msg = new GELFMessage(fakeData);
        assertEquals(GELFMessage.TYPE_GZIP, msg.getGELFType());
    }

    @Test
    public void testGetGELFTypeDetectsChunkedMessage() throws Exception {
        byte[] fakeData = new byte[20];
        fakeData[0] = (byte) 0x1e;
        fakeData[1] = (byte) 0x0f;

        GELFMessage msg = new GELFMessage(fakeData);
        assertEquals(GELFMessage.TYPE_CHUNKED, msg.getGELFType());
    }

    @Test
    public void testGetGELFTypeDetectsUncompressedMessage() throws Exception {
        byte[] fakeData = new byte[20];
        fakeData[0] = (byte) 0x1f;
        fakeData[1] = (byte) 0x3c;

        GELFMessage msg = new GELFMessage(fakeData);
        assertEquals(GELFMessage.TYPE_UNCOMPRESSED, msg.getGELFType());
    }

    @Test
    public void testGetJSONFromZLIBCompressedMessage() throws Exception {
        GELFMessage msg = new GELFMessage(TestHelper.zlibCompress(GELF_JSON));
        assertEquals(GELF_JSON, msg.getJSON());
    }

    @Test
    public void testGetJSONFromGZIPCompressedMessage() throws Exception {
        GELFMessage msg = new GELFMessage(TestHelper.gzipCompress(GELF_JSON));
        assertEquals(GELF_JSON, msg.getJSON());
    }

    @Test
    public void testGetJSONFromUncompressedMessage() throws Exception {
        byte[] text = GELF_JSON.getBytes("UTF-8");
        byte[] message = new byte[text.length+2];
        message[0] = (byte) 0x1f;
        message[1] = (byte) 0x3c;

        // Copy text behind magic bytes identifying uncompressed message.
        System.arraycopy(text, 0, message, 2, text.length);

        GELFMessage msg = new GELFMessage(message);
        assertEquals(GELF_JSON, msg.getJSON());
    }

    @Test
    public void testAsChunk() throws Exception {
        String id = "foobar01";
        int seqNum = 1;
        int seqCnt = 5;
        byte[] data = TestHelper.gzipCompress(GELF_JSON);

        GELFMessage msg = new GELFMessage(TestHelper.buildGELFMessageChunk(id, seqNum, seqCnt, data));
        
        assertEquals(TestHelper.toHex(id), msg.asChunk().getId());
        assertEquals(seqNum, msg.asChunk().getSequenceNumber());
        assertEquals(seqCnt, msg.asChunk().getSequenceCount());
        assertArrayEquals(data, msg.asChunk().getData());
    }

}