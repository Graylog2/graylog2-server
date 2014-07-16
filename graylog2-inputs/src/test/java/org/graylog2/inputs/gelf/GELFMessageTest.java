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

import org.graylog2.inputs.TestHelper;
import org.graylog2.inputs.gelf.gelf.GELFMessage;
import org.graylog2.inputs.gelf.gelf.GELFMessageChunk;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

/**
 * @author lennart.koopmann
 */
public class GELFMessageTest {

    public final static String GELF_JSON = "{\"message\":\"foo\",\"host\":\"bar\",\"_lol_utf8\":\"\u00FC\"}";

    @Test
    public void testGetGELFTypeDetectsZLIBCompressedMessage() throws Exception {
        byte[] fakeData = new byte[20];
        fakeData[0] = (byte) 0x78;
        fakeData[1] = (byte) 0x9c;

        GELFMessage msg = new GELFMessage(fakeData);
        assertEquals(GELFMessage.Type.ZLIB, msg.getGELFType());
    }

    @Test
    public void testGetGELFTypeDetectsGZIPCompressedMessage() throws Exception {
        byte[] fakeData = new byte[20];
        fakeData[0] = (byte) 0x1f;
        fakeData[1] = (byte) 0x8b;

        GELFMessage msg = new GELFMessage(fakeData);
        assertEquals(GELFMessage.Type.GZIP, msg.getGELFType());
    }

    @Test
    public void testGetGELFTypeDetectsChunkedMessage() throws Exception {
        byte[] fakeData = new byte[20];
        fakeData[0] = (byte) 0x1e;
        fakeData[1] = (byte) 0x0f;

        GELFMessage msg = new GELFMessage(fakeData);
        assertEquals(GELFMessage.Type.CHUNKED, msg.getGELFType());
    }

    @Test
    public void testGetGELFTypeDetectsUncompressedMessage() throws Exception {
        byte[] fakeData = new byte[20];
        fakeData[0] = (byte) '{';
        fakeData[1] = (byte) '\n';

        GELFMessage msg = new GELFMessage(fakeData);
        assertEquals(GELFMessage.Type.UNCOMPRESSED, msg.getGELFType());
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

        GELFMessage msg = new GELFMessage(text);
        assertEquals(GELF_JSON, msg.getJSON());
    }

    @Test
    public void testGelfMessageChunkCreation() throws Exception {
        String id = "foobar01";
        int seqNum = 1;
        int seqCnt = 5;
        byte[] data = TestHelper.gzipCompress(GELF_JSON);

        GELFMessage msg = new GELFMessage(TestHelper.buildGELFMessageChunk(id, seqNum, seqCnt, data));
        GELFMessageChunk chunk = new GELFMessageChunk(msg, null);
        
        assertEquals(TestHelper.toHex(id), chunk.getId());
        assertEquals(seqNum, chunk.getSequenceNumber());
        assertEquals(seqCnt, chunk.getSequenceCount());
        assertArrayEquals(data, chunk.getData());
    }

}