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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import de.huxhorn.sulky.ulid.ULID;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

// Fill the first 16 bits of the ULIDs random section (16_bit_uint_random) with
// a sequence number that reflects the order in which messages were received by an input.
//
// The sequence numbers on messages are ints and don't fit into 16 bits.
// We remember a messages' first sequence number (subtrahend) in a size limited cache per input and timestamp.
// The sequence number will then be subtracted with this subtrahend.
// Thus will the first received message (for a certain input and timestamp) start with a sequence of 0 (see [1]).
//
// [1]
// Since our message processing is multithreaded, messages can pass each other during processing.
// This means that the first recorded sequence number can be higher than the one of later messages.
// To account for this, we simply add a constant (REORDERING_GAP) to prevent negative messageSequenceNrs.
// Therefor the first sequence does not start with 0, but with REORDERING_GAP.
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
 */
@Singleton
public class MessageULIDGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(MessageULIDGenerator.class);

    private final Cache<String, Integer> sequenceNrCache;
    private final ULID ulid;
    static final long ULID_RANDOM_MSB_MASK = 0xFFFFL;

    static final int REORDERING_GAP = 5000;

    @Inject
    public MessageULIDGenerator(ULID ulid) {
        this.ulid = ulid;

        sequenceNrCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .initialCapacity(2000)
                .build();
    }

    public String createULID(Message message) {
        try {
            return createULID(message.getSourceInputId(), message.getTimestamp().getMillis(), message.getSequenceNr());
        } catch (Exception e) {
            LOG.error("Exception while creating ULID.", e);
            return ulid.nextULID(message.getTimestamp().getMillis());
        }
    }

    @VisibleForTesting
    String createULID(String inputId, long timestamp, int sequenceNr) {
        final String key = inputId + timestamp;
        final int subtrahend = sequenceNrCache.get(key, k -> sequenceNr);

        if (sequenceNr == subtrahend) {
            LOG.trace("Added new timestamp <{}> for input <{}> to cache. Seq <{}>", timestamp, inputId, sequenceNr);
        }

        final ULID.Value nextUlid = ulid.nextValue(timestamp);
        final long leastSignificantBits = nextUlid.getLeastSignificantBits();

        final long msbWithZeroedRandom = timestamp << 16;
        long messageSequenceNr = sequenceNr - subtrahend + REORDERING_GAP;

        // If our multithreaded message processing reorders the messages by more than REORDERING_GAP,
        // the messageSequenceNr can become negative.
        // This can also happen if the sequenceNr counter in a MessageInput wraps.
        // We handle this by updating the sequenceNrCache and setting the messageSequenceNr accordingly.
        if (messageSequenceNr < 0) {
            LOG.warn("Got negative message sequence number ({} -> {}). Sort order might be wrong.", subtrahend, sequenceNr);
            messageSequenceNr = REORDERING_GAP;
            sequenceNrCache.put(key, sequenceNr);
        // If we receive more than 60535 messages with the same timestamp and input, they will exhaust the 16 bit of space in the ULID.
        } else if (messageSequenceNr >= ULID_RANDOM_MSB_MASK) {
            LOG.warn("Message sequence number <{}> input <{}> timestamp <{}> does not fit into ULID ({} >= 65535). Sort order might be wrong.",
                    sequenceNr, inputId, timestamp, messageSequenceNr);
            messageSequenceNr %= ULID_RANDOM_MSB_MASK;
        }
        final ULID.Value sequencedUlid = new ULID.Value(msbWithZeroedRandom | messageSequenceNr, leastSignificantBits);
        return sequencedUlid.toString();
    }
}
