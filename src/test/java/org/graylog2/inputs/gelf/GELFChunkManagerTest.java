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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.graylog2.Core;
import org.graylog2.TestHelper;
import org.graylog2.gelf.GELFChunkManager;
import org.graylog2.gelf.GELFChunks;
import org.graylog2.gelf.GELFMessage;
import org.graylog2.gelf.GELFMessageChunk;
import org.graylog2.gelf.MessageParseException;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.junit.Test;

import com.google.common.primitives.Bytes;

public class GELFChunkManagerTest {

    private Core server = null;

    @Test
    public void testIsCompleteDetectsAsNotComplete() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        long msgId = 1234l;
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, new byte[1])));
        
        assertFalse(mgr.isComplete(msgId));
    }
    
    @Test
    public void testIsCompleteReceives() throws Exception {
        final AtomicBoolean rc = new AtomicBoolean(false);
        
        GELFChunkManager mgr = new GELFChunkManager(server){
            protected void receiveAssembledMessage(GELFMessageChunk chunk, GELFChunks messageChunks) throws BufferOutOfCapacityException ,MessageParseException {
                GELFMessage msg = messageChunks.assembleGELFMessage();
                
                rc.set(true);
            };
        };
 
        long msgId = 12345l;
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 2, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, new byte[1])));
        
        assertTrue(rc.get());
    }
    
    @Test
    public void testIsCompleteWithEmptyChunkMap() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
        assertFalse(mgr.isComplete(1l));
    }

    @Test
    public void testIsOutdated() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        long msgId = 123456l;
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, new byte[1])));
        
        try { Thread.sleep((GELFChunkManager.SECONDS_VALID+1)*1000); } catch (InterruptedException e) { /* trololol */ }
        
        assertTrue(mgr.isOutdated(msgId));
    }
    
    @Test
    public void testIsNotOutdated() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        long msgId = 1234567l;
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, new byte[1])));
        
        try { Thread.sleep((GELFChunkManager.SECONDS_VALID-2)*1000); } catch (InterruptedException e) { /* trololol */ }
        
        assertFalse(mgr.isOutdated(msgId));
    }
    

    @Test
    public void testDropMessageWithExistingMessage() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        long msgId = 12345678l;
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 2, new byte[1])));
        
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(123456789l, 1, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(123456789l, 0, 3, new byte[1])));
        
        mgr.dropMessage(msgId);
        assertFalse(mgr.hasMessage(msgId));
    }
    
    @Test
    public void testDropMessageWithNotExistingMessage() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
 
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(99l, 1, 3, new byte[1])));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(99l, 0, 3, new byte[1])));
        
        mgr.dropMessage(11l);
        assertFalse(mgr.hasMessage(11));
    }
    
    @Test
    public void testDropMessageWitEmptyChunkMap() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
        mgr.dropMessage(11);
    }

    @Test
    public void testChunksToByteArray() throws Exception {
 
        byte[] b1 = "nothing".getBytes();
        byte[] b2 = "tosee12".getBytes(); // all chunks except last one must have uniform size
        byte[] b3 = "here".getBytes();
        
        byte[] b12 = Bytes.concat(b1, b2);
        final byte[] expected = Bytes.concat(b12, b3);
        final AtomicBoolean rc = new AtomicBoolean();
        
        GELFChunkManager mgr = new GELFChunkManager(server){
            protected void receiveAssembledMessage(GELFMessageChunk chunk, GELFChunks messageChunks) throws BufferOutOfCapacityException ,MessageParseException {
                GELFMessage msg = messageChunks.assembleGELFMessage();
                assertArrayEquals(expected, Arrays.copyOfRange(msg.getPayload(), msg.getOffset(), msg.getOffset()+msg.getLength()) );
                
                rc.set(true);
            };
        };

        long msgId = 123400l;
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, b1)));
        // And another message, to confuse stuff.
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(4321l, 2, 3, b1)));

        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 2, 3, b3)));
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 1, 3, b2)));
        
        assertTrue(rc.get());
    }

    @Test
    public void testInsert() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server) ;

        long msgId = 99l;
        
        assertFalse(mgr.hasMessage(msgId));
        
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(msgId, 0, 3, new byte[1])));

        assertTrue(mgr.hasMessage(msgId));
    }

    @Test
    public void testHumanReadableChunkMap() throws Exception {
        GELFChunkManager mgr = new GELFChunkManager(server);
        mgr.insert(new GELFMessageChunk(TestHelper.buildGELFMessageChunk(123l, 0, 3, new byte[1])));
        mgr.humanReadableChunkMap();
    }
    
    @Test
    public void testHumanReadableChunkMapWithEmptyChunkMap() {
        GELFChunkManager mgr = new GELFChunkManager(server);
        mgr.humanReadableChunkMap();
    }

}