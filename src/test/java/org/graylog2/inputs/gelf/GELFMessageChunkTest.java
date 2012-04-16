/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.inputs.gelf;

import java.io.IOException;
import org.graylog2.TestHelper;
import org.graylog2.Tools;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart.koopmann
 */
public class GELFMessageChunkTest {

    public final static String GELF_JSON = "{\"message\":\"foo\",\"host\":\"bar\",\"_lol_utf8\":\"ü\"}";

    
    private GELFMessageChunk buildChunk() throws Exception {
        String id = "lolwat67";
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
        assertEquals(TestHelper.toHex("lolwat67"), buildChunk().getId());
    }

    @Test
    public void testGetData() throws Exception  {
        assertArrayEquals(TestHelper.gzipCompress(GELF_JSON), buildChunk().getData());
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