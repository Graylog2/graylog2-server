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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class GELFMessageChunk {
    
    /**
     * The start byte of the sequence number
     */
    public static final int HEADER_PART_SEQNUM_START = 10;

    /**
     * The length of the sequence number
     */
    public static final int HEADER_PART_SEQNUM_LENGTH = 1;

    /**
     * The start byte of the sequence count
     */
    public static final int HEADER_PART_SEQCNT_START = 11;

    /**
     * The length of the sequence count
     */
    public static final int HEADER_PART_SEQCNT_LENGTH = 1;

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

    private String id;
    private byte[] data = new byte[1];
    private int sequenceNumber = -1;
    private int sequenceCount = -1;
    private int arrival = -1;

    final byte[] payload;

    private final int offset;

    private final int length;

    public GELFMessageChunk(final byte[] payload) {
        this(payload,0,payload.length);
    }

    public GELFMessageChunk(final byte[] payload, int offset, int length) {
        this.offset = offset;
        this.length = length;
        this.payload = payload;

        if (length < HEADER_TOTAL_LENGTH) {
            throw new IllegalArgumentException("This GELF message chunk is too short. Cannot even contain the required header.");
        }

        read();
    }

    public GELFMessageChunk(final GELFMessage msg) {
        this(msg.getPayload());
    }

    public int getArrival() {
        return this.arrival;
    }

    public String getId() {
        return this.id;
    }

    public byte[] getData() {
        return this.data;
    }

    public int getSequenceCount() {
        return this.sequenceCount;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    private void read() {
        extractId();
        extractSequenceCount();
        extractSequenceNumber();
        extractData();
        this.arrival = (int) (System.currentTimeMillis()/1000);
    }

    private String extractId() {
        if (this.id == null) {
            this.id = Long.toHexString(ByteBuffer.wrap(payload, offset+HEADER_PART_HASH_START, HEADER_PART_HASH_LENGTH).order(ByteOrder.BIG_ENDIAN).getLong() );
        }

        return this.id;
    }

    // lol duplication
    private void extractSequenceNumber() {
        if (this.sequenceNumber == -1) {
            final int seqNum = this.sliceInteger(HEADER_PART_SEQNUM_START, HEADER_PART_SEQNUM_LENGTH);
            if (seqNum >= 0) {
                this.sequenceNumber = seqNum;
            } else {
                throw new IllegalStateException("Could not extract sequence number");
            }
        }
    }

    // lol duplication
    private void extractSequenceCount() {
        if (this.sequenceCount == -1) {
            final int seqCnt = this.sliceInteger(HEADER_PART_SEQCNT_START, HEADER_PART_SEQCNT_LENGTH);
            if (seqCnt >= 0) {
                this.sequenceCount = seqCnt;
            } else {
                throw new IllegalStateException("Could not extract sequence count");
            }
        }
    }

    private void extractData() {
        this.data = slice(HEADER_TOTAL_LENGTH); // Slice everything starting at the total header length.
    }

    private int sliceInteger(final int start, final int length) {
        if (length==1)
            return payload[offset + start]; // which are 100% cases =)
        
        String tmp = "";
        for (int i = 0; i < length; i++) {
            tmp = tmp.concat(Integer.toString(payload[offset+ i+start]));
        }
        return Integer.parseInt(tmp);
    }

    private byte[] slice(final int cutOffAt) {
        return Arrays.copyOfRange(payload, offset+cutOffAt, offset+length);
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
