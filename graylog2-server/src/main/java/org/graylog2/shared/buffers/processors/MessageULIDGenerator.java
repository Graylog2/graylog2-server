/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.buffers.processors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.huxhorn.sulky.ulid.ULID;
import org.graylog2.plugin.Message;
import org.graylog2.shared.journal.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageULIDGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(MessageULIDGenerator.class);

    private final Cache<String, Integer> sequenceNrCache;
    private final ULID ulid;
    private static final long RANDOM_MSB_MASK = 0xFFFFL;
    private static final int OFFSET_GAP = 500;

    @Inject
    public MessageULIDGenerator(ULID ulid, Journal journal) {
        this.ulid = ulid;

        sequenceNrCache = Caffeine.newBuilder()
                .maximumSize(100)
                .initialCapacity(100)
                .build();
    }

    public Message addULID(Message message) {
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

        final ULID.Value nextUlid = ulid.nextValue(message.getTimestamp().getMillis());
        final long mostSignificantBits = nextUlid.getMostSignificantBits();
        final long leastSignificantBits = nextUlid.getLeastSignificantBits();

        final long msbWithZeroedRandom = mostSignificantBits & ~RANDOM_MSB_MASK;
        int messageSequenceNr = message.getSequenceNr();
        if (messageSequenceNr >= RANDOM_MSB_MASK) {
            LOG.warn("Message sequence number does not fit into ULID ({} >= 65535). Sort order might be wrong.", messageSequenceNr);
            messageSequenceNr %= RANDOM_MSB_MASK;
        }
        final ULID.Value sequencedUlid = new ULID.Value(msbWithZeroedRandom + messageSequenceNr, leastSignificantBits);
        message.addField(Message.FIELD_GL2_MESSAGE_ID, sequencedUlid.toString());
        return message;
    }
}
