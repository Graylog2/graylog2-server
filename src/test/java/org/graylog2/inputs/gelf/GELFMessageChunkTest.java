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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.graylog2.TestHelper;
import org.graylog2.gelf.GELFMessageChunk;
import org.graylog2.plugin.Tools;
import org.junit.Test;


public class GELFMessageChunkTest {

    public final static String GELF_JSON = "{\"message\":\"foo\",\"host\":\"bar\",\"_lol_utf8\":\"ü\"}";

    
    private GELFMessageChunk buildChunk() throws Exception {
        long id = 67;
        int seqNum = 3;
        int seqCnt = 4;
        byte[] data = TestHelper.gzipCompress(GELF_JSON);

        return new GELFMessageChunk(TestHelper.buildGELFMessageChunk(id, seqNum, seqCnt, data));
    }
    
    @Test
    public void testGetArrival() throws Exception {
        int timestamp = Tools.getUTCTimestamp();
        GELFMessageChunk chunk = buildChunk();
        assertTrue((timestamp - chunk.getArrival()) < 5); // delta shmelta
    }

    @Test
    public void testGetId() throws Exception {      
        assertEquals(67l, buildChunk().getId());
    }

    @Test
    public void testGetData() throws Exception  {
        assertArrayEquals(TestHelper.gzipCompress(GELF_JSON), getData(buildChunk()));
    }
    

    private byte[] getData(GELFMessageChunk buildChunk) throws IOException
    {
        byte[] data = new byte[ buildChunk.getBodyLength() ];
        
        buildChunk.writeBody(data,0);
        
        return data;
    }

    @Test
    public void testGetSequenceCount() throws Exception  {
        assertEquals(4, buildChunk().getSequenceCount());
    }

    @Test
    public void testGetSequenceNumber() throws Exception  {
        assertEquals(3, buildChunk().getSequenceNumber());
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(buildChunk().toString());
    }

}