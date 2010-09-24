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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ChunkedGELFMessage.java: Sep 18, 2010 3:37:43 PM
 *
 * [description]
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ChunkedGELFMessage extends GELFMessage {

    // <SequenceNumber, Chunk>
    private HashMap<Integer, GELFClientChunk> chunkMap = new HashMap<Integer, GELFClientChunk>();
    
    private int sequenceCount = -1;
    private String hash;

    public void insertChunk(GELFClientChunk chunk) throws InvalidGELFChunkException, ForeignGELFChunkException {
        chunk.checkStructure();

        // Update sequenceCount and hash if this is the first chunk (first chunk if sequenceCount not set yet)
        if (this.sequenceCount == -1) {
            this.sequenceCount = chunk.getSequenceCount();
            this.hash = chunk.getHash();
        }

        if (!chunk.getHash().equals(this.hash)) {
            throw new ForeignGELFChunkException("Chunk does not belong to this message.");
        }

        // Insert chunk.
        this.chunkMap.put(chunk.getSequenceNumber(), chunk);
    }

    public HashMap<Integer, GELFClientChunk> getChunkMap() {
        return chunkMap;
    }

    public int getSequenceCount() {
        return this.sequenceCount;
    }

    public boolean isComplete () {
        if (sequenceCount == chunkMap.size()) {
            return true;
        }

        return false;
    }
    
    public Collection<GELFClientChunk> getChunks() {
        return this.chunkMap.values();
    }

    public byte[] getData() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Sort chunkMap first
        List<Integer> sortedList = new ArrayList<Integer>();
        sortedList.addAll(this.chunkMap.keySet());
        Collections.sort(sortedList);

        Iterator<Integer> iter = sortedList.iterator();
        while (iter.hasNext()) {
            GELFClientChunk chunk = this.chunkMap.get(iter.next());
            System.out.println("ADDING TO BUFFER: " + chunk.getSequenceNumber());
            out.write(chunk.getData(), 0, chunk.getData().length);
        }

        return out.toByteArray();
    }

}