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

package org.graylog2.gelf;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class GELFMessageChunk {
    
    /**
     * The start byte of the sequence number
     */
    public static final int HEADER_PART_SEQNUM_START = 10;

    /**
     * The start byte of the sequence count
     */
    public static final int HEADER_PART_SEQCNT_START = 11;

    /**
     * The start byte of the message hash
     */
    public static final int HEADER_PART_HASH_START = 2;

    /**
     * The length of the message hash
     */
    public static final int HEADER_PART_HASH_LENGTH = 8;

    /**
     * The total length of the header.
     */
    public static final int HEADER_TOTAL_LENGTH = 12;

    private long id;
    private int sequenceNumber = -1;
    private int sequenceCount = -1;
    private int arrival = -1;

    final byte[] payload;

    private final int offset;

    private final int length;

    public GELFMessageChunk(final byte[] payload) throws MessageParseException {
        this(payload,0,payload.length);
    }

    public GELFMessageChunk(final byte[] payload, int offset, int length) throws MessageParseException {
        this.offset = offset;
        this.length = length;
        this.payload = payload;

        if (length < HEADER_TOTAL_LENGTH) {
            throw new MessageParseException("This GELF message chunk is too short. Cannot even contain the required header.");
        }
        
        read();

    }

    public GELFMessageChunk(final GELFMessage msg) throws MessageParseException {
        this(msg.getPayload(), msg.getOffset(), msg.getLength());
    }

    public int getArrival() {
        return this.arrival;
    }

    public long getId() {
        return this.id;
    }

    public int writeBody(byte[] to, int ofs) {
        int bodyLength = getBodyLength();
        System.arraycopy(payload, offset+HEADER_TOTAL_LENGTH, to, ofs, bodyLength);
        return bodyLength;
    }
    
    public int getBodyLength() {
        return length-HEADER_TOTAL_LENGTH;
    }

    public int getSequenceCount() {
        return this.sequenceCount;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }
    
    public boolean isLastChunk() {
        return this.sequenceNumber == this.sequenceCount - 1;
    }

    private void read() throws MessageParseException {
        extractId();
        extractSequenceCount();
        extractSequenceNumber();
        this.arrival = (int) (System.currentTimeMillis()/1000);
    }

    private void extractId() {
        this.id = ByteBuffer.wrap(payload, offset+HEADER_PART_HASH_START, HEADER_PART_HASH_LENGTH).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    // lol duplication
    private void extractSequenceNumber() throws MessageParseException {
        sequenceNumber = this.sliceByte(HEADER_PART_SEQNUM_START);
        if (sequenceNumber < 0) {
            throw new MessageParseException("Could not extract sequence number/negative : "+sequenceNumber);
        }
    }

    // lol duplication
    private void extractSequenceCount() throws MessageParseException {
        sequenceCount = this.sliceByte(HEADER_PART_SEQCNT_START);
        if (sequenceCount < 0) {
            throw new MessageParseException("Could not extract sequence count or it is negative: "+sequenceCount);
        }
    }

    private byte sliceByte(final int start) {
        return payload[offset + start]; // which are 100% cases =)
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("ID: ");
        sb.append(this.id);
        sb.append("\tSequence: ");
        sb.append(this.sequenceNumber+1); // +1 for readability: 1/2 not 0/2
        sb.append("/");
        sb.append(this.sequenceCount);
        sb.append("\tArrival: ");
        sb.append(this.arrival);
        sb.append("\tData size: ");
        sb.append(this.length);

        return sb.toString();
    }

}
