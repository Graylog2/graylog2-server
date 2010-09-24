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

/**
 * GELFClientChunk.java: Sep 20, 2010 8:16:11 PM
 *
 * Representing a GELF message chunk
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFClientChunk {

    private String hash = null;
    private byte[] data = null;
    private int sequenceNumber = -1;
    private int sequenceCount = -1;;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(int sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public boolean checkStructure() throws InvalidGELFChunkException {
        if (this.hash == null || this.hash.length() == 0) {
            throw new InvalidGELFChunkException("Invalid GELF chunk: No hash set.");
        }

        if (this.sequenceNumber < 0) {
            throw new InvalidGELFChunkException("Invalid GELF chunk: No sequence number set.");
        }

        if (this.sequenceCount <= 0) {
            throw new InvalidGELFChunkException("Invalid GELF chunk: No sequence count set.");
        }

        if (this.sequenceNumber >= this.sequenceCount) {
            throw new InvalidGELFChunkException("Invalid GELF chunk: Sequence number must be lower than sequence count.");
        }
        
        return true;
    }

}
