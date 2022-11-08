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

@Singleton
public class MessageULIDGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(MessageULIDGenerator.class);

    private final Cache<String, Integer> sequenceNrCache;
    private final ULID ulid;
    static final long RANDOM_MSB_MASK = 0xFFFFL;
    static final int OFFSET_GAP = 1000;

    @Inject
    public MessageULIDGenerator(ULID ulid) {
        this.ulid = ulid;

        sequenceNrCache = Caffeine.newBuilder()
                .maximumSize(100)
                .initialCapacity(100)
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
        // Fill the first 16 bits of the ULIDs random section with a sequence number that reflects the order
        // in which messages were received by an input.
        //
        // To make the sequence number fit into 16 bits, we subtract it with itself.
        // Thus will the first received message start with a sequence of 0.
        // We remember this initial sequence number (subtrahend) in a size limited cache per input and timestamp.
        // This ensures that the following messages (with the same timestamp and input) are subtracted with
        // the same sequence number, and therefore keep their order.
        //
        // Since our message processing is multi threaded, messages can pass each other during processing.
        // This means that the first recorded sequence number can be higher than the one of later messages.
        // To account for this, we simply add a constant (OFFSET_GAP) to prevent negative messageSequenceNrs.

        final String key = inputId + timestamp;
        final Integer subtrahend = sequenceNrCache.get(key, k -> sequenceNr);

        if (sequenceNr == subtrahend) {
            LOG.trace("Added new timestamp <{}> for input <{}> to cache. Seq <{}>", timestamp, inputId, sequenceNr);
        }

        final ULID.Value nextUlid = ulid.nextValue(timestamp);
        final long leastSignificantBits = nextUlid.getLeastSignificantBits();

        final long msbWithZeroedRandom = timestamp << 16;
        long messageSequenceNr = sequenceNr - subtrahend + OFFSET_GAP;

        // If the sequenceNr counter in a MessageInput wraps while we're processing the same timestamp
        // the messageSequenceNr can become negative. We handle this by updating the sequenceNrCache and
        // setting the messageSequenceNr accordingly.
        if (messageSequenceNr < 0) {
            LOG.warn("Message sequence number wrapped ({} -> {}). Sort order might be wrong.", subtrahend, sequenceNr);
            messageSequenceNr = OFFSET_GAP;
            sequenceNrCache.put(key, sequenceNr);
        } else if (messageSequenceNr >= RANDOM_MSB_MASK) {
            LOG.warn("Message sequence number <{}> input <{}> timestamp <{}> does not fit into ULID ({} >= 65535). Sort order might be wrong.", sequenceNr, inputId, timestamp, messageSequenceNr);
            messageSequenceNr %= RANDOM_MSB_MASK;
        }
        final ULID.Value sequencedUlid = new ULID.Value(msbWithZeroedRandom + messageSequenceNr, leastSignificantBits);
        return sequencedUlid.toString();
    }
}
