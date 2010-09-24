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

import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ChunkedGELFMessageTest.java: Sep 24, 2010 4:10:24 PM
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ChunkedGELFMessageTest {

    /**
     * Test of insertChunk method, of class ChunkedGELFMessage.
     */
    @Test
    public void testInsertChunk() throws InvalidGELFChunkException, ForeignGELFChunkException {
        ChunkedGELFMessage message = new ChunkedGELFMessage();

        byte[] data = {1,3,5};
        String hash = "123abc";

        int howManyChunks = 10;
        int i = 0;
        while (i < howManyChunks) {
            GELFClientChunk chunk = new GELFClientChunk();
            chunk.setHash(hash);
            chunk.setData(data);
            chunk.setSequenceCount(howManyChunks);
            chunk.setSequenceNumber(i);

            message.insertChunk(chunk);
            i++;
        }

        HashMap<Integer, GELFClientChunk> chunks = message.getChunkMap();

        assertTrue(chunks.size() == howManyChunks);
    }

    /**
     * Test of getSequenceCount method, of class ChunkedGELFMessage.
     */
    @Test
    public void testGetSequenceCount() throws InvalidGELFChunkException, ForeignGELFChunkException {
        ChunkedGELFMessage message = new ChunkedGELFMessage();

        byte[] data = {1,3,5};
        String hash = "123abc";

        // Insert a chunk.
        GELFClientChunk chunk1 = new GELFClientChunk();
        chunk1.setHash(hash);
        chunk1.setSequenceNumber(0);
        chunk1.setSequenceCount(3);
        message.insertChunk(chunk1);

        /*
         * Insert another chunk with a sequence count different to the first one.
         * This should not change the result of getSequenceCount();
         */
        GELFClientChunk chunk2 = new GELFClientChunk();
        chunk2.setHash(hash);
        chunk2.setSequenceNumber(1);
        chunk2.setSequenceCount(5);
        message.insertChunk(chunk2);

        assertEquals(3, message.getSequenceCount());
    }

    /**
     * Test of isComplete method, of class ChunkedGELFMessage.
     */
    @Test
    public void testIsComplete() throws InvalidGELFChunkException, ForeignGELFChunkException {
        ChunkedGELFMessage message = new ChunkedGELFMessage();

        byte[] data = {1,3,5};
        String hash = "123abc";

        int howManyChunks = 20;
        int i = 0;
        while (i < howManyChunks-1) {
            GELFClientChunk chunk = new GELFClientChunk();
            chunk.setHash(hash);
            chunk.setData(data);
            chunk.setSequenceCount(howManyChunks);
            chunk.setSequenceNumber(i);

            message.insertChunk(chunk);
            i++;
        }

        // One chunk is missing.
        assertFalse(message.isComplete());

        // Add the last missing chunk of this message.
        GELFClientChunk correctChunk = new GELFClientChunk();
        correctChunk.setHash(hash);
        correctChunk.setSequenceNumber(howManyChunks-1);
        correctChunk.setSequenceCount(howManyChunks);
        message.insertChunk(correctChunk);

        // Message is complete now.
        assertTrue(message.isComplete());
    }

    /**
     * Test that the insert() method does not accept chunks of other messages
     */
    @Test (expected=ForeignGELFChunkException.class)
    public void testInsertDoesNotAcceptForeignChunks() throws InvalidGELFChunkException, ForeignGELFChunkException {
        ChunkedGELFMessage message = new ChunkedGELFMessage();

        // Insert a chunk.
        GELFClientChunk correctChunk = new GELFClientChunk();
        correctChunk.setHash("123abc");
        correctChunk.setSequenceNumber(0);
        correctChunk.setSequenceCount(2);
        message.insertChunk(correctChunk);

        // Insert a chunk with another hash (= from another message)
        GELFClientChunk foreignChunk = new GELFClientChunk();
        foreignChunk.setHash("123whoami");
        foreignChunk.setSequenceNumber(0);
        foreignChunk.setSequenceCount(2);
        message.insertChunk(foreignChunk);
    }

}