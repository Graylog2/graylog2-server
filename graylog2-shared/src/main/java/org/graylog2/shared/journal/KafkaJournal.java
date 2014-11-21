/*
 * Copyright 2014 TORCH GmbH
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
 */
package org.graylog2.shared.journal;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import kafka.common.OffsetOutOfRangeException;
import kafka.common.TopicAndPartition;
import kafka.log.CleanerConfig;
import kafka.log.Log;
import kafka.log.LogConfig;
import kafka.log.LogManager;
import kafka.message.ByteBufferMessageSet;
import kafka.message.Message;
import kafka.message.MessageAndOffset;
import kafka.message.MessageSet;
import kafka.utils.KafkaScheduler;
import kafka.utils.SystemTime$;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Map$;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.transform;

@Singleton
public class KafkaJournal {
    private static final Logger log = LoggerFactory.getLogger(KafkaJournal.class);
    private final LogManager logManager;
    private final Log kafkaLog;
    private long readOffset = 0L; // TODO read from persisted store

    @Inject
    public KafkaJournal(@Named("spoolDirectory") String spoolDir) {

        // TODO all of these configuration values need tweaking
        // these are the default values as per kafka 0.8.1.1
        final LogConfig defaultConfig =
                new LogConfig(
                        1024 * 1024,
                        Long.MAX_VALUE,
                        Long.MAX_VALUE,
                        Long.MAX_VALUE,
                        Long.MAX_VALUE,
                        Long.MAX_VALUE,
                        Integer.MAX_VALUE,
                        1024 * 1024,
                        4096,
                        60 * 1000,
                        24 * 60 * 60 * 1000L,
                        0.5,
                        false
                );
        // these are the default values as per kafka 0.8.1.1, except we don't turn on the cleaner
        final CleanerConfig cleanerConfig =
                new CleanerConfig(
                        1,
                        4 * 1024 * 1024L,
                        0.9d,
                        1024 * 1024,
                        32 * 1024 * 1024,
                        5 * 1024 * 1024L,
                        TimeUnit.SECONDS.toMillis(15),
                        false,
                        "MD5");
        logManager = new LogManager(
                new File[]{new File(spoolDir)},
                Map$.MODULE$.<String, LogConfig>empty(),
                defaultConfig,
                cleanerConfig,
                TimeUnit.SECONDS.toMillis(60),
                TimeUnit.SECONDS.toMillis(60),
                TimeUnit.SECONDS.toMillis(60),
                new KafkaScheduler(2, "relay", false),
                SystemTime$.MODULE$);

        final TopicAndPartition topicAndPartition = new TopicAndPartition("messagejournal", 0);
        final Option<Log> messageLog = logManager.getLog(topicAndPartition);
        if (messageLog.isEmpty()) {
            kafkaLog = logManager.createLog(topicAndPartition, logManager.defaultConfig());
        } else {
            kafkaLog = messageLog.get();
        }
        log.info("Initialized Kafka based journal at {}", spoolDir);
    }

    /**
     * Creates an opaque object which can be passed to {@link #write(java.util.List)} for a bulk journal write.
     *
     * @param idBytes      a byte array which represents the key for the entry
     * @param messageBytes the journal entry's payload, i.e. the message itself
     * @return a journal entry to be passed to {@link #write(java.util.List)}
     */
    public Entry createEntry(byte[] idBytes, byte[] messageBytes) {
        return new Entry(messageBytes, idBytes);
    }

    /**
     * Writes the list of entries to the journal.
     * @param entries
     * @return the last position written to in the journal
     */
    public long write(List<Entry> entries) {
        final long[] payloadSize = {0L};
        final ByteBufferMessageSet messageSet = new ByteBufferMessageSet(JavaConversions.asScalaBuffer(transform(
                entries,
                new Function<Entry, Message>() {
                    @Nullable
                    @Override
                    public Message apply(Entry entry) {
                        payloadSize[0] += entry.messageBytes.length;
                        return new Message(entry.messageBytes, entry.idBytes);
                    }
                })));

        final Log.LogAppendInfo appendInfo = kafkaLog.append(messageSet, true);
        log.info("Wrote {} messages to journal: {} bytes, log position {} to {}",
                 entries.size(), payloadSize[0], appendInfo.firstOffset(), appendInfo.lastOffset());
        return appendInfo.lastOffset();
    }

    /**
     * Writes a single message to the journal and returns the new write position
     * @param idBytes
     * @param messageBytes
     * @return the last position written to in the journal
     */
    public long write(byte[] idBytes, byte[] messageBytes) {
        final Entry journalEntry = createEntry(idBytes, messageBytes);
        return write(Collections.singletonList(journalEntry));
    }

    public List<JournalReadEntry> read() {
        final long maxOffset = readOffset + 1;
        final List<JournalReadEntry> messages = Lists.newArrayListWithCapacity((int) (maxOffset - readOffset));
        try {
            final MessageSet messageSet = kafkaLog.read(readOffset, 10 * 1024, Option.<Object>apply(maxOffset));

            final Iterator<MessageAndOffset> iterator = messageSet.iterator();
            while (iterator.hasNext()) {
                final MessageAndOffset messageAndOffset = iterator.next();
                final ByteBuffer payload = messageAndOffset.message().payload();
                messages.add(new JournalReadEntry(payload, messageAndOffset.offset()));
                readOffset = messageAndOffset.nextOffset();
            }

        } catch (OffsetOutOfRangeException e) {
            log.warn("Offset out of range, no messages available starting at offset {}", readOffset);
        }
        return messages;
    }

    public static class Entry {
        private final byte[] idBytes;
        private final byte[] messageBytes;

        public Entry(byte[] idBytes, byte[] messageBytes) {
            this.idBytes = idBytes;
            this.messageBytes = messageBytes;
        }
    }

    public static class JournalReadEntry {

        private final ByteBuffer payload;
        private final long offset;

        public JournalReadEntry(ByteBuffer payload, long offset) {
            this.payload = payload;
            this.offset = offset;
        }

        public long getOffset() {
            return offset;
        }

        public ByteBuffer getPayload() {
            return payload;
        }
    }
}