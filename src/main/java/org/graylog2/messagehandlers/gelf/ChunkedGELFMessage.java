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
import java.util.Map;
import java.util.TreeMap;

/**
 * ChunkedGELFMessage.java: Sep 18, 2010 3:37:43 PM
 *
 * A GELF message containing of several chunks
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ChunkedGELFMessage extends GELFMessage {

    /*
     * <SequenceNumber, Chunk>
     */
    private Map<Integer, GELFClientChunk> chunkMap = new TreeMap<Integer, GELFClientChunk>();
    
    private int sequenceCount = -1;
    private String hash;

    /**
     * Add a chunk of this message
     *
     * @param chunk a chunk belonging to this message
     * @throws InvalidGELFChunkException when chunk is malformed
     * @throws ForeignGELFChunkException when chunk belongs to a different message
     */
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

    /**
     * Get all chunks of this message
     * @return 
     */
    public Map<Integer, GELFClientChunk> getChunkMap() {
        return chunkMap;
    }

    /**
     * Get the sequence count of this message: How many chunks does this message have?
     * @return
     */
    public int getSequenceCount() {
        return this.sequenceCount;
    }

    /**
     * Is this message complete? Have all chunks been collected?
     * @return boolean
     */
    public boolean isComplete() {
        if (sequenceCount == chunkMap.size()) {
            return true;
        }

        return false;
    }

    /**
     * Get the timestamp of when the first chunk of this message arrived.
     * @return UNIX timestamp
     * @throws EmptyGELFMessageException
     */
    public int getFirstChunkArrival() throws EmptyGELFMessageException {
        if (!chunkMap.containsKey(0)) {
            throw new EmptyGELFMessageException();
        }

        return chunkMap.get(0).getArrival();
    }

    /**
     * Get the hash/ID of this message.
     * @return the hash/id of this message
     */
    public String getHash() {
        return this.hash;
    }

    /**
     * Get the correctly arranged and combined data of all chunks. Message must
     * be complete to call this.
     * @return a byte array representing this message's data
     * @throws IncompleteGELFMessageException if message is incomplete
     */
    public byte[] getData() throws IncompleteGELFMessageException {
        if (chunkMap.isEmpty() || !this.isComplete()) {
            throw new IncompleteGELFMessageException();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (GELFClientChunk chunk : chunkMap.values()) {
            out.write(chunk.getData(), 0, chunk.getData().length);
        }

        return out.toByteArray();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message <");
        sb.append(hash);
        sb.append("> ");
        sb.append(getChunkMap());
        return sb.toString();
    }

}