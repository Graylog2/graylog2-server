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
import org.graylog2.shared.journal.KafkaJournal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageULIDGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(MessageULIDGenerator.class);

    private final Cache<String, Long> journalOffsetCache;
    private final ULID ulid;
    private final KafkaJournal journal;
    private static final long RANDOM_MSB_MASK = 0xFFFFL;
    private static final int OFFSET_GAP = 500;

    @Inject
    public MessageULIDGenerator(ULID ulid, Journal journal) {
        this.ulid = ulid;
        if (journal instanceof KafkaJournal) {
            this.journal = (KafkaJournal) journal;
        } else {
            this.journal = null;
        }

        journalOffsetCache = Caffeine.newBuilder()
                .maximumSize(100)
                .initialCapacity(100)
                .build();
    }

    public Message addULID(Message message) {
        final long messageTimestamp = message.getTimestamp().getMillis();

        // If we are running without a journal (NoopJournal) create a regular ULID
        if (journal == null) {
            message.addField(Message.FIELD_GL2_MESSAGE_ID, ulid.nextULID(messageTimestamp));
            return message;
        }

        // Otherwise we build a ULID that includes a message sequence number
        final String key = message.getSourceInputId() + messageTimestamp;
        final Long subtrahend = journalOffsetCache.get(key, k -> journal.getCommittedOffset());

        ULID.Value nextUlid = ulid.nextValue(messageTimestamp);
        long mostSignificantBits = nextUlid.getMostSignificantBits();
        long leastSignificantBits = nextUlid.getLeastSignificantBits();

        // Fill the first 16 bits of the ULIDs random section with a sequence number that reflects the order
        // in which a message was received by an input.
        // The sequence number is created by using each message's journalOffset.
        //
        // To fit the number into 16 bits, we keep it small by subtracting it with the
        // currently recorded journal committed offset.
        //
        // To ensure we subtract the same offset for a batch of messages that were received with identical timestamps,
        // we remember the offsets in a size limited cache per input and timestamp.
        //
        // Finally, we need to deal with the fact that the journals commited offset
        // is updated by multiple concurrent processor threads. This could mean that
        // the committed offset is actually slightly higher than message offset.
        // Thus we simply add a gap constant (OFFSET_GAP) to prevent us from creating a negative messageSequenceNr.

        long msbWithZeroedRandom = mostSignificantBits & ~RANDOM_MSB_MASK;
        long messageSequenceNr = message.getJournalOffset() - subtrahend + OFFSET_GAP;
        if (messageSequenceNr >= RANDOM_MSB_MASK) {
            LOG.warn("Message sequence number does not fit into ULID ({} >= 65535). Sort order might be wrong.", messageSequenceNr);
            messageSequenceNr %= RANDOM_MSB_MASK;
        }
        final ULID.Value sequencedUlid = new ULID.Value(msbWithZeroedRandom + messageSequenceNr, leastSignificantBits);
        message.addField(Message.FIELD_GL2_MESSAGE_ID, sequencedUlid.toString());
        return message;
    }
}
