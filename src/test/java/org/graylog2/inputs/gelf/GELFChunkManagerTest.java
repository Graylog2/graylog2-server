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

import org.graylog2.gelf.GELFChunkManager;
import org.graylog2.gelf.GELFMessageChunk;
import com.google.common.primitives.Bytes;
import org.graylog2.Core;
import org.graylog2.TestHelper;
import org.junit.Test;
import static org.junit.Assert.*;

public class GELFChunkManagerTest {

    private Core server = null;

    @Test
    public void testIsCompleteDetectsAsNotComplete() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        String msgId = "lolwat67";
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, new byte[1])));
        
        assertFalse(mgr.isComplete(TestHelper.toHex(msgId)));
    }
    
    @Test
    public void testIsCompleteDetectsAsComplete() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        String msgId = "foobar00";
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 2, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, new byte[1])));
        
        assertTrue(mgr.isComplete(TestHelper.toHex(msgId)));
    }
    
    @Test
    public void testIsCompleteWithEmptyChunkMap() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
        assertFalse(mgr.isComplete(TestHelper.toHex("lolwat")));
    }

    @Test
    public void testIsOutdated() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        String msgId = "lolwat67";
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, new byte[1])));
        
        try { Thread.sleep((GELFChunkManager.SECONDS_VALID+1)*1000); } catch (InterruptedException e) { /* trololol */ }
        
        assertTrue(mgr.isOutdated(TestHelper.toHex(msgId)));
    }
    
    @Test
    public void testIsNotOutdated() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        String msgId = "lolwat67";
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, new byte[1])));
        
        try { Thread.sleep((GELFChunkManager.SECONDS_VALID-2)*1000); } catch (InterruptedException e) { /* trololol */ }
        
        assertFalse(mgr.isOutdated(TestHelper.toHex(msgId)));
    }
    
    @Test
    public void testIsNotOutdatedWithEmptyChunkMap() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
        assertFalse(mgr.isOutdated(TestHelper.toHex("lolwat")));
    }

    @Test
    public void testDropMessageWithExistingMessage() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        String msgId = "foobaz11";
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 2, new byte[1])));
        
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk("lollol99", 1, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk("lollol99", 0, 3, new byte[1])));
        
        mgr.dropMessage(TestHelper.toHex(msgId));
        assertFalse(mgr.hasMessage(TestHelper.toHex(msgId)));
    }
    
    @Test
    public void testDropMessageWithNotExistingMessage() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk("lollol99", 1, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk("lollol99", 0, 3, new byte[1])));
        
        mgr.dropMessage(TestHelper.toHex("something"));
        assertFalse(mgr.hasMessage(TestHelper.toHex("something")));
    }
    
    @Test
    public void testDropMessageWitEmptyChunkMap() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
        mgr.dropMessage("lol");
    }

    @Test
    public void testChunksToByteArray() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        byte[] b1 = TestHelper.gzipCompress("nothing");
        byte[] b2 = TestHelper.gzipCompress("tosee");
        byte[] b3 = TestHelper.gzipCompress("here");
        
        byte[] b12 = Bytes.concat(b1, b2);
        byte[] expected = Bytes.concat(b12, b3);
        
        String msgId = "foobar00";
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, b1)));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, b2)));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 2, 3, b3)));
        
        // And another message, to confuse stuff.
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk("hahahaha", 2, 3, b1)));
        
        assertArrayEquals(expected, mgr.chunksToByteArray(TestHelper.toHex(msgId)));
    }

    @Test
    public void testInsert() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);

        String msgId = "lolwat99";
        
        assertFalse(mgr.hasMessage(TestHelper.toHex(msgId)));
        
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));

        assertTrue(mgr.hasMessage(TestHelper.toHex(msgId)));
    }

    @Test
    public void testHumanReadableChunkMap() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk("ohaithar", 0, 3, new byte[1])));
        mgr.humanReadableChunkMap();
    }
    
    @Test
    public void testHumanReadableChunkMapWithEmptyChunkMap() {
        GELFChunkManager mgr = new GELFChunkManager(server);
        mgr.humanReadableChunkMap();
    }

}