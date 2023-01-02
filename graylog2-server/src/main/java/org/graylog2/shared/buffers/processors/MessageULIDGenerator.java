/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.buffers.processors;

import com.google.common.annotations.VisibleForTesting;
import de.huxhorn.sulky.ulid.ULID;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

// Fill the first 32 bits of the ULIDs random section with
// a sequence number that reflects the order in which messages were received by an input.
/*
    The ULID binary layout for reference

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                      32_bit_uint_time_high                    |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |     16_bit_uint_time_low      |       16_bit_uint_random      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                       32_bit_uint_random                      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                       32_bit_uint_random                      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

    The modified ULID binary layout

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                      32_bit_uint_time_high                    |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |     16_bit_uint_time_low      |       16_bit_uint_seq_msb     |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |     16_bit_uint_seq_lsb       |       16_bit_uint_random      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                       32_bit_uint_random                      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
@Singleton
public class MessageULIDGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(MessageULIDGenerator.class);
    private final ULID ulid;
    @Inject
    public MessageULIDGenerator(ULID ulid) {
        this.ulid = ulid;
    }

    public String createULID(Message message) {
        try {
            return createULID(message.getTimestamp().getMillis(), message.getSequenceNr());
        } catch (Exception e) {
            LOG.error("Exception while creating ULID.", e);
            return ulid.nextULID(message.getTimestamp().getMillis());
        }
    }

    @VisibleForTesting
    String createULID(long timestamp, int sequenceNr) {
        final ULID.Value nextULID = ulid.nextValue(timestamp);

        final long msbSeq = sequenceNr >>> 16;
        final long lsbSeq = sequenceNr & 0xFFFF;
        final long msbWithZeroedRandom = nextULID.getMostSignificantBits() & 0xFFFF_FFFF_FFFF_0000L;
        final long lsbWithZeroedRandom = nextULID.getLeastSignificantBits() & 0x0000_FFFF_FFFF_FFFFL;

        final ULID.Value sequencedULID = new ULID.Value(msbWithZeroedRandom | msbSeq, lsbWithZeroedRandom | (lsbSeq << 48));
        return sequencedULID.toString();
    }
}
