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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.security.SecureRandom;

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
    private final SecureRandom random;

    @Inject
    public MessageULIDGenerator(ULID ulid) {
        this.ulid = ulid;
        random = new SecureRandom();
    }

    public String createULID(Message message) {
        checkTimestamp(message.getTimestamp().getMillis());
        try {
            return createULID(message.getTimestamp().getMillis(), message.getSequenceNr());
        } catch (Exception e) {
            LOG.error("Exception while creating ULID.", e);
            return ulid.nextULID(message.getTimestamp().getMillis());
        }
    }

    @VisibleForTesting
    String createULID(long timestamp, int sequenceNr) {
        final long msbSeq = sequenceNr >>> 16;
        final long lsbSeq = sequenceNr & 0xFFFF;

        final long msbWithoutRandom = timestamp << 16;
        final long lsbWithoutRandom = lsbSeq << 48;
        final long nextRandom = random.nextLong();

        final ULID.Value sequencedULID = new ULID.Value(msbWithoutRandom | msbSeq, lsbWithoutRandom | (nextRandom >>> 16));
        return sequencedULID.toString();
    }

    private void checkTimestamp(long timestamp) {
        // Check extracted from de.huxhorn.sulky.ulid.ULID
        final long TIMESTAMP_OVERFLOW_MASK = 0xFFFF_0000_0000_0000L;
        if ((timestamp & TIMESTAMP_OVERFLOW_MASK) != 0) {
            throw new IllegalArgumentException("ULID does not support timestamps after +10889-08-02T05:31:50.655Z!");
        }
    }
}
