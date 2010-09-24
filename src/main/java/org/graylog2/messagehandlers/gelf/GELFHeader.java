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

import java.io.IOException;

/**
 * GELFHeader.java: Sep 18, 2010 6:07:45 PM
 *
 * Representing the header of chunked GELF messages
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFHeader {

    private byte[] rawHeader;

    private String hash = null;
    private int sequenceNumber = -1;
    private int sequenceCount = -1;

    public static final int HEADER_PART_SEQNUM_START = 34;
    public static final int HEADER_PART_SEQNUM_LENGTH = 2;
    public static final int HEADER_PART_SEQCNT_START = 36;
    public static final int HEADER_PART_SEQCNT_LENGTH = 2;
    public static final int HEADER_PART_HASH_START = 2;
    public static final int HEADER_PART_HASH_LENGTH = 32;

    public GELFHeader(byte[] rawHeader) {
        this.rawHeader = rawHeader;
    }

    private int extract(int start, int length) {
        String tmp = "";
        for (int i = 0; i < length; i++) {
            tmp = tmp.concat(Integer.toString(this.rawHeader[i+start]));
        }
        return Integer.parseInt(tmp);
    }

    public String getHash() throws InvalidGELFHeaderException, IOException {

        if (this.hash == null) {
            String tmp = "";
            for (int i = 0; i < GELFHeader.HEADER_PART_HASH_LENGTH; i++) {
                tmp += Integer.toString( ( this.rawHeader[i+GELFHeader.HEADER_PART_HASH_START] & 0xff ) + 0x100, 16).substring( 1 );
            }
            this.hash = tmp;
        }

        return this.hash;
    }

    public int getSequenceNumber() throws InvalidGELFHeaderException {
        // Lazy calculate sequence number.
        if (this.sequenceNumber == -1) {
            int seqNum = this.extract(GELFHeader.HEADER_PART_SEQNUM_START, GELFHeader.HEADER_PART_SEQNUM_LENGTH);
            if (seqNum >= 0) {
                this.sequenceNumber = seqNum;
            } else {
                throw new InvalidGELFHeaderException("Could not extract sequence number");
            }
        }

        return this.sequenceNumber;
    }

    public int getSequenceCount() throws InvalidGELFHeaderException {
        // Lazy calculate sequence count.
        if (this.sequenceCount == -1) {
            int seqCnt = this.extract(GELFHeader.HEADER_PART_SEQCNT_START, GELFHeader.HEADER_PART_SEQCNT_LENGTH);
            if (seqCnt >= 0) {
                this.sequenceCount = seqCnt;
            } else {
                throw new InvalidGELFHeaderException("Could not extract sequence count");
            }
        }

        return this.sequenceCount;
    }

}