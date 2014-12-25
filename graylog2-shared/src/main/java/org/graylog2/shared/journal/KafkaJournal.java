/**
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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import kafka.common.KafkaException;
import kafka.common.OffsetOutOfRangeException;
import kafka.common.TopicAndPartition;
import kafka.log.CleanerConfig;
import kafka.log.Log;
import kafka.log.LogConfig;
import kafka.log.LogManager;
import kafka.log.LogSegment;
import kafka.message.ByteBufferMessageSet;
import kafka.message.Message;
import kafka.message.MessageAndOffset;
import kafka.message.MessageSet;
import kafka.utils.KafkaScheduler;
import kafka.utils.Time;
import kafka.utils.Utils;
import org.graylog2.plugin.ThrottleState;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.metrics.HdrTimer;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Map$;
import scala.runtime.AbstractFunction1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.*;
import static org.graylog2.plugin.Tools.bytesToHex;

@Singleton
public class KafkaJournal extends AbstractIdleService implements Journal {
    // this exists so we can use JodaTime's millis provider in tests.
    // kafka really only cares about the milliseconds() method in here
    private static final Time JODA_TIME = new Time() {
        @Override
        public long milliseconds() {
            return DateTimeUtils.currentTimeMillis();
        }

        @Override
        public long nanoseconds() {
            return System.nanoTime();
        }

        @Override
        public void sleep(long ms) {
            Uninterruptibles.sleepUninterruptibly(ms, MILLISECONDS);
        }
    };
    private static final Logger log = LoggerFactory.getLogger(KafkaJournal.class);
    private static final long DEFAULT_COMMITTED_OFFSET = Long.MIN_VALUE;
    private final LogManager logManager;
    private final Log kafkaLog;
    private final File committedReadOffsetFile;
    private final AtomicLong committedOffset = new AtomicLong(DEFAULT_COMMITTED_OFFSET);
    private final ScheduledExecutorService scheduler;
    private final long retentionSize;
    private final EventBus eventBus;
    private final MetricRegistry metricRegistry;
    private ProcessBuffer pb;
    private final Timer writeTime;

    private final Timer readTime;
    private final KafkaScheduler kafkaScheduler;
    private final Meter messagesWritten;
    private final Meter messagesRead;

    private final OffsetFileFlusher offsetFlusher;
    private final DirtyLogFlusher dirtyLogFlusher;
    private final RecoveryCheckpointFlusher recoveryCheckpointFlusher;
    private final LogRetentionCleaner logRetentionCleaner;
    private final ThrottleStateUpdater throttleStateUpdater;

    private long nextReadOffset = 0L;
    private long lastWriteOffset = Long.MIN_VALUE;
    private ScheduledFuture<?> throttleUpdaterFuture;
    private ScheduledFuture<?> checkpointFlusherFuture;
    private ScheduledFuture<?> dirtyLogFlushFuture;
    private ScheduledFuture<?> logRetentionFuture;
    private ScheduledFuture<?> offsetFlusherFuture;

    @Inject
    public KafkaJournal(@Named("message_journal_dir") String journalDirName,
                        @Named("scheduler") ScheduledExecutorService scheduler,
                        @Named("message_journal_segment_size") int segmentSize,
                        @Named("message_journal_max_size") long retentionSize,
                        @Named("message_journal_max_age") Duration retentionAge,
                        EventBus eventBus,
                        MetricRegistry metricRegistry,
                        ProcessBuffer pb) {
        this.scheduler = scheduler;
        this.retentionSize = retentionSize;
        this.eventBus = eventBus;
        this.metricRegistry = metricRegistry;
        this.pb = pb;

        this.messagesWritten = metricRegistry.meter(name(this.getClass(), "messagesWritten"));
        this.messagesRead = metricRegistry.meter(name(this.getClass(), "messagesRead"));

        writeTime = metricRegistry.register(name(this.getClass(), "writeTime"), new HdrTimer(1, TimeUnit.MINUTES, 1));
        readTime = metricRegistry.register(name(this.getClass(), "readTime"), new HdrTimer(1, TimeUnit.MINUTES, 1));

        // these are the default values as per kafka 0.8.1.1
        final LogConfig defaultConfig =
                new LogConfig(
                        // segmentSize: The soft maximum for the size of a segment file in the log
                        segmentSize,
                        // segmentMs: The soft maximum on the amount of time before a new log segment is rolled
                        Long.MAX_VALUE,
                        // flushInterval: The number of messages that can be written to the log before a flush is forced
                        Long.MAX_VALUE,
                        // flushMs: The amount of time the log can have dirty data before a flush is forced
                        Long.MAX_VALUE,
                        // retentionSize: The approximate total number of bytes this log can use
                        retentionSize,
                        // retentionMs: The age approximate maximum age of the last segment that is retained
                        retentionAge.getMillis(),
                        // maxMessageSize: The maximum size of a message in the log
                        Integer.MAX_VALUE,
                        // maxIndexSize: The maximum size of an index file
                        1024 * 1024,
                        // indexInterval: The approximate number of bytes between index entries
                        4096,
                        // fileDeleteDelayMs: The time to wait before deleting a file from the filesystem
                        60 * 1000,
                        // deleteRetentionMs: The time to retain delete markers in the log. Only applicable for logs that are being compacted.
                        24 * 60 * 60 * 1000L,
                        // minCleanableRatio: The ratio of bytes that are available for cleaning to the bytes already cleaned
                        0.5,
                        // compact: Should old segments in this log be deleted or deduplicated?
                        false
                );
        // these are the default values as per kafka 0.8.1.1, except we don't turn on the cleaner
        // Cleaner really is log compaction with respect to "deletes" in the log.
        // we never insert a message twice, at least not on purpose, so we do not "clean" logs, ever.
        final CleanerConfig cleanerConfig =
                new CleanerConfig(
                        1,
                        4 * 1024 * 1024L,
                        0.9d,
                        1024 * 1024,
                        32 * 1024 * 1024,
                        5 * 1024 * 1024L,
                        SECONDS.toMillis(15),
                        false,
                        "MD5");
        final File journalDirectory = new File(journalDirName);
        if (!journalDirectory.exists() && !journalDirectory.mkdirs()) {
            log.error("Cannot create journal directory at {}, please check the permissions",
                      journalDirectory.getAbsolutePath());
        }
        // TODO add check for directory, etc
        committedReadOffsetFile = new File(journalDirectory, "graylog2-committed-read-offset");
        try {
            if (!committedReadOffsetFile.createNewFile()) {
                final String line = Files.readFirstLine(committedReadOffsetFile, Charsets.UTF_8);
                // the file contains the last offset graylog2 has successfully processed.
                // thus the nextReadOffset is one beyond that number
                if (line != null) {
                    committedOffset.set(Long.parseLong(line.trim()));
                    nextReadOffset = committedOffset.get() + 1;
                }
            }
        } catch (IOException e) {
            log.error("Cannot access offset file", e);
            Throwables.propagate(e);
        }
        try {
            kafkaScheduler = new KafkaScheduler(2,
                                                "kafka-journal-scheduler-",
                                                false); // TODO make thread count configurable
            kafkaScheduler.startup();
            logManager = new LogManager(
                    new File[]{journalDirectory},
                    Map$.MODULE$.<String, LogConfig>empty(),
                    defaultConfig,
                    cleanerConfig,
                    SECONDS.toMillis(60),
                    SECONDS.toMillis(60),
                    SECONDS.toMillis(60),
                    kafkaScheduler,
                    JODA_TIME);

            final TopicAndPartition topicAndPartition = new TopicAndPartition("messagejournal", 0);
            final Option<Log> messageLog = logManager.getLog(topicAndPartition);
            if (messageLog.isEmpty()) {
                kafkaLog = logManager.createLog(topicAndPartition, logManager.defaultConfig());
            } else {
                kafkaLog = messageLog.get();
            }
            log.info("Initialized Kafka based journal at {}", journalDirName);
            setupKafkaLogMetrics(metricRegistry);

            offsetFlusher = new OffsetFileFlusher();
            dirtyLogFlusher = new DirtyLogFlusher();
            recoveryCheckpointFlusher = new RecoveryCheckpointFlusher();
            logRetentionCleaner = new LogRetentionCleaner();
            throttleStateUpdater = new ThrottleStateUpdater();
        } catch (KafkaException e) {
            // most likely failed to grab lock
            log.error("Unable to start logmanager.", e);
            throw new RuntimeException(e);
        }

    }

    private void setupKafkaLogMetrics(final MetricRegistry metricRegistry) {
        metricRegistry.register(name(KafkaJournal.class, "size"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return kafkaLog.size();
            }
        });
        metricRegistry.register(name(KafkaJournal.class, "logEndOffset"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return kafkaLog.logEndOffset();
            }
        });
        metricRegistry.register(name(KafkaJournal.class, "numberOfSegments"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return kafkaLog.numberOfSegments();
            }
        });
        metricRegistry.register(name(KafkaJournal.class, "unflushedMessages"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return kafkaLog.unflushedMessages();
            }
        });
        metricRegistry.register(name(KafkaJournal.class, "recoveryPoint"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return kafkaLog.recoveryPoint();
            }
        });
        metricRegistry.register(name(KafkaJournal.class, "lastFlushTime"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return kafkaLog.lastFlushTime();
            }
        });
    }


    /**
     * Creates an opaque object which can be passed to {@link #write(java.util.List)} for a bulk journal write.
     *
     * @param idBytes      a byte array which represents the key for the entry
     * @param messageBytes the journal entry's payload, i.e. the message itself
     * @return a journal entry to be passed to {@link #write(java.util.List)}
     */
    @Override
    public Entry createEntry(byte[] idBytes, byte[] messageBytes) {
        return new Entry(idBytes, messageBytes);
    }

    /**
     * Writes the list of entries to the journal.
     *
     * @param entries journal entries to be written
     * @return the last position written to in the journal
     */
    @Override
    public long write(List<Entry> entries) {
        try (Timer.Context ignored = writeTime.time()) {
            long payloadSize = 0L;

            final List<Message> messages = Lists.newArrayList();
            for (final Entry entry : entries) {
                final byte[] messageBytes = entry.getMessageBytes();
                final byte[] idBytes = entry.getIdBytes();

                payloadSize += messageBytes.length;
                messages.add(new Message(messageBytes, idBytes));

                if (log.isTraceEnabled()) {
                    log.trace("Message {} contains bytes {}", bytesToHex(idBytes), bytesToHex(messageBytes));
                }
            }

            final ByteBufferMessageSet messageSet = new ByteBufferMessageSet(JavaConversions.asScalaBuffer(messages));

            final Log.LogAppendInfo appendInfo = kafkaLog.append(messageSet, true);
            lastWriteOffset = appendInfo.lastOffset();
            log.debug("Wrote {} messages to journal: {} bytes, log position {} to {}",
                      entries.size(), payloadSize, appendInfo.firstOffset(), lastWriteOffset);
            messagesWritten.mark(entries.size());
            return lastWriteOffset;
        }
    }

    /**
     * Writes a single message to the journal and returns the new write position
     *
     * @param idBytes      byte array congaing the message id
     * @param messageBytes encoded message payload
     * @return the last position written to in the journal
     */
    @Override
    public long write(byte[] idBytes, byte[] messageBytes) {
        final Entry journalEntry = createEntry(idBytes, messageBytes);
        return write(Collections.singletonList(journalEntry));
    }

    @Override
    public List<JournalReadEntry> read(long requestedMaximumCount) {
        return read(nextReadOffset, requestedMaximumCount);
    }

    public List<JournalReadEntry> read(long readOffset, long requestedMaximumCount) {

        // Always read at least one!
        final long maximumCount = Math.max(1, requestedMaximumCount);
        long maxOffset = readOffset + maximumCount;
        final List<JournalReadEntry> messages = Lists.newArrayListWithCapacity((int) (maximumCount));

        try (Timer.Context ignored = readTime.time()) {
            final long logStartOffset = getLogStartOffset();

            if (readOffset < logStartOffset) {
                log.error(
                        "Read offset {} before start of log at {}, starting to read from the beginning of the journal.",
                        readOffset,
                        logStartOffset);
                readOffset = logStartOffset;
                maxOffset = readOffset + maximumCount;
            }
            log.debug("Requesting to read a maximum of {} messages (or 5MB) from the journal, offset interval [{}, {})",
                      maximumCount, readOffset, maxOffset);

            // TODO benchmark and make read-ahead strategy configurable for performance tuning
            final MessageSet messageSet = kafkaLog.read(readOffset,
                                                        5 * 1024 * 1024,
                                                        Option.<Object>apply(maxOffset));

            final Iterator<MessageAndOffset> iterator = messageSet.iterator();
            long firstOffset = Long.MIN_VALUE;
            long lastOffset = Long.MIN_VALUE;
            long totalBytes = 0;
            while (iterator.hasNext()) {
                final MessageAndOffset messageAndOffset = iterator.next();

                if (firstOffset == Long.MIN_VALUE) firstOffset = messageAndOffset.offset();
                // always remember the last seen offset for debug purposes below
                lastOffset = messageAndOffset.offset();

                final byte[] payloadBytes = Utils.readBytes(messageAndOffset.message().payload());
                if (log.isTraceEnabled()) {
                    final byte[] keyBytes = Utils.readBytes(messageAndOffset.message().key());
                    log.trace("Read message {} contains {}", bytesToHex(keyBytes), bytesToHex(payloadBytes));
                }
                totalBytes += payloadBytes.length;
                messages.add(new JournalReadEntry(payloadBytes, messageAndOffset.offset()));
                // remember where to read from
                nextReadOffset = messageAndOffset.nextOffset();
            }
            if (messages.isEmpty()) {
                log.debug("No messages available to read for offset interval [{}, {}).", readOffset, maxOffset);
            } else {
                log.debug(
                        "Read {} messages, total payload size {}, from journal, offset interval [{}, {}], requested read at {}",
                        messages.size(),
                        totalBytes,
                        firstOffset,
                        lastOffset,
                        readOffset);
            }

        } catch (OffsetOutOfRangeException e) {
            // TODO how do we recover from this? the exception doesn't contain the next valid offset :(
            log.warn("Offset out of range, no messages available starting at offset {}", readOffset);
        } catch (Exception e) {
            // the scala code does not declare the IOException in kafkaLog.read() so we can't catch it here
            // sigh.
            //noinspection ConstantConditions
            if (e instanceof ClosedByInterruptException) {
                log.debug("Interrupted while reading from journal, during shutdown this is harmless and ignored.", e);
            } else {
                throw e;
            }

        }
        messagesRead.mark(messages.size());
        return messages;
    }

    /**
     * Upon fully processing, and persistently storing, a batch of messages, the system should mark the message with the
     * highest offset as committed. A background job will write the last position to disk periodically.
     *
     * @param offset the offset of the latest committed message
     */
    @Override
    public void markJournalOffsetCommitted(long offset) {
        long prev;
        // the caller will not care about offsets going backwards, so we need to make sure we don't backtrack
        int i = 0;
        do {
            prev = committedOffset.get();
            // at least warn if this spins often, that would be a sign of very high contention, which should not happen
            if (++i % 10 == 0) {
                log.warn("Committing journal offset spins {} times now, this might be a bug. Continuing to try update.",
                         i);
            }
        } while (!committedOffset.compareAndSet(prev, Math.max(offset, prev)));
    }

    /**
     * A Java transliteration of what the scala implementation does, which unfortunately is declared as private
     */
    protected void flushDirtyLogs() {
        log.debug("Checking for dirty logs to flush...");

        final Set<Map.Entry<TopicAndPartition, Log>> entries = JavaConversions.mapAsJavaMap(logManager.logsByTopicPartition()).entrySet();
        for (final Map.Entry<TopicAndPartition, Log> topicAndPartitionLogEntry : entries) {
            final TopicAndPartition topicAndPartition = topicAndPartitionLogEntry.getKey();
            final Log kafkaLog = topicAndPartitionLogEntry.getValue();
            final long timeSinceLastFlush = JODA_TIME.milliseconds() - kafkaLog.lastFlushTime();
            try {
                log.debug(
                        "Checking if flush is needed on {} flush interval {} last flushed {} time since last flush: {}",
                        topicAndPartition.topic(),
                        kafkaLog.config().flushInterval(),
                        kafkaLog.lastFlushTime(),
                        timeSinceLastFlush);
                if (timeSinceLastFlush >= kafkaLog.config().flushMs()) {
                    kafkaLog.flush();
                }
            } catch (Exception e) {
                log.error("Error flushing topic " + topicAndPartition.topic(), e);
            }
        }
    }

    @Override
    protected void startUp() throws Exception {
        // do NOT let Kafka's LogManager create its management threads, we will run them ourselves.
        // The problem is that we can't reliably decorate or subclass them, so we will peel the methods out and call
        // them ourselves. it sucks, but i haven't found a better way yet.
        // /* don't call */ logManager.startup();

        // flush dirty logs regularly
        dirtyLogFlushFuture = scheduler.scheduleAtFixedRate(dirtyLogFlusher,
                                                            SECONDS.toMillis(30),
                                                            logManager.flushCheckMs(),
                                                            MILLISECONDS);

        // write recovery checkpoint files
        checkpointFlusherFuture = scheduler.scheduleAtFixedRate(recoveryCheckpointFlusher,
                                                               SECONDS.toMillis(30),
                                                               logManager.flushCheckpointMs(),
                                                               MILLISECONDS);


        // custom log retention cleaner
        logRetentionFuture = scheduler.scheduleAtFixedRate(logRetentionCleaner,
                                                           SECONDS.toMillis(30),
                                                           logManager.retentionCheckMs(),
                                                           MILLISECONDS);

        // regularly write the currently committed read offset to disk
        offsetFlusherFuture = scheduler.scheduleAtFixedRate(offsetFlusher, 1, 1, SECONDS);

        throttleUpdaterFuture = scheduler.scheduleAtFixedRate(throttleStateUpdater, 1, 1, SECONDS);
    }

    @Override
    protected void shutDown() throws Exception {
        throttleUpdaterFuture.cancel(false);
        offsetFlusherFuture.cancel(false);
        logRetentionFuture.cancel(false);
        checkpointFlusherFuture.cancel(false);
        dirtyLogFlushFuture.cancel(false);

        kafkaScheduler.shutdown();
        logManager.shutdown();
        // final flush
        offsetFlusher.run();
    }

    public int cleanupLogs() {
        try {
            return logRetentionCleaner.call();
        } catch (Exception e) {
            log.error("Unable to delete expired segments.", e);
            return 0;
        }
    }

    // default visibility for tests
    public Iterable<LogSegment> getSegments() {
        return JavaConversions.asJavaIterable(kafkaLog.logSegments());
    }

    /**
     * Returns the journal size in bytes, exluding index files.
     * @return journal size in bytes
     */
    public long size() {
        return kafkaLog.size();
    }

    /**
     * Returns the number of segments this journal consists of.
     * @return number of segments
     */
    public int numberOfSegments() {
        return kafkaLog.numberOfSegments();
    }

    /**
     * Returns the highest journal offset that has been writting to persistent storage by Graylog2.
     *<p>
     *     Every message at an offset prior to this one can be considered as processed and does not need to be held in
     *     the journal any longer. By default Graylog2 will try to aggressively flush the journal to consume a smaller
     *     amount of disk space.
     *</p>
     * @return the offset of the last message which has been successfully processed.
     */
    public long getCommittedReadOffset() {
        return committedOffset.get();
    }

    /**
     * Discards all data in the journal prior to the given offset.
     * @param offset offset to truncate to, so that no offset in the journal is larger than this.
     */
    public void truncateTo(long offset) {
        kafkaLog.truncateTo(offset);
    }

    /**
     * Returns the first valid offset in the entire journal.
     * @return first offset
     */
    public long getLogStartOffset() {
        final Iterable<LogSegment> logSegments = JavaConversions.asJavaIterable(kafkaLog.logSegments());
        final LogSegment segment = Iterables.getFirst(logSegments, null);
        if (segment == null) {
            return 0;
        }
        return segment.baseOffset();
    }

    /**
     * returns the offset for the next value to be inserted in the entire journal.
     * @return the next offset value (last valid offset is this number - 1)
     */
    public long getLogEndOffset() {
        return kafkaLog.logEndOffset();
    }

    /**
     * The ThrottleStateUpdater publishes the current state buffer state of the journal to other interested parties,
     * chiefly the ThrottleableTransports.
     *
     * <p>
     *     It only includes the necessary information to make a decision about whether to throttle parts of the system,
     *     but does not send "throttle" commands. This allows for a flexible approach in picking a throttling strategy.
     * </p>
     * <p>
     *     The implementation expects to be called once per second to have a rough estimate about the events per second,
     *     over the last second.
     * </p>
     */
    public class ThrottleStateUpdater implements Runnable {
        private boolean firstRun = true;

        private long logStartOffset;
        private long logEndOffset;
        private long previousLogEndOffset;
        private long previousReadOffset;
        private long currentReadOffset;
        private long currentTs;
        private long prevTs;

        @Override
        public void run() {
            final ThrottleState throttleState = new ThrottleState();
            final long committedOffset = KafkaJournal.this.committedOffset.get();

            prevTs = currentTs;
            currentTs = System.nanoTime();

            previousLogEndOffset = logEndOffset;
            previousReadOffset = currentReadOffset;
            logStartOffset = getLogStartOffset();
            logEndOffset = getLogEndOffset() - 1; // -1 because getLogEndOffset is the next offset that gets assigned
            currentReadOffset = KafkaJournal.this.nextReadOffset - 1; // just to make it clear which field we read

            // for the first run, don't send an update, there's no previous data available to calc rates
            if (firstRun) {
                firstRun = false;
                return;
            }

            throttleState.appendEventsPerSec = (long) Math.floor((logEndOffset - previousLogEndOffset) / ((currentTs - prevTs) / 1.0E09));
            throttleState.readEventsPerSec = (long) Math.floor((currentReadOffset - previousReadOffset) / ((currentTs - prevTs) / 1.0E09));

            throttleState.journalSize = size();
            throttleState.journalSizeLimit = retentionSize;

            throttleState.processBufferCapacity = pb.getRemainingCapacity();

            if (committedOffset == DEFAULT_COMMITTED_OFFSET) {
                // nothing committed at all, the entire log is uncommitted, or completely empty.
                throttleState.uncommittedJournalEntries = size() == 0 ? size() : logEndOffset - logStartOffset;
            } else {
                throttleState.uncommittedJournalEntries = logEndOffset - committedOffset;
            }
            log.debug("ThrottleState: {}", throttleState);
            eventBus.post(throttleState);
        }
    }


    public class OffsetFileFlusher implements Runnable {
        @Override
        public void run() {
            // Do not write the file if committedOffset has never been updated.
            if (committedOffset.get() == DEFAULT_COMMITTED_OFFSET) {
                return;
            }
            try (final FileOutputStream fos = new FileOutputStream(committedReadOffsetFile)) {
                fos.write(String.valueOf(committedOffset.get()).getBytes(Charsets.UTF_8));
                // flush stream
                fos.flush();
                // actually sync to disk
                fos.getFD().sync();
            } catch (SyncFailedException e) {
                log.error("Cannot sync " + committedReadOffsetFile.getAbsolutePath() + " to disk. Continuing anyway," +
                                  " but there is no guarantee that the file has been written.", e);
            } catch (IOException e) {
                log.error("Cannot write " + committedReadOffsetFile.getAbsolutePath() + " to disk.", e);
            }
        }
    }

    /**
     * Java implementation of the Kafka log retention cleaner.
     */
    public class LogRetentionCleaner implements Runnable, Callable<Integer> {

        private final Logger loggerForCleaner = LoggerFactory.getLogger(LogRetentionCleaner.class);

        @Override
        public void run() {
            try {
                call();
            } catch (Exception e) {
                loggerForCleaner.error("Unable to delete expired segments. Will try again.", e);
            }
        }

        @Override
        public Integer call() throws Exception {
            loggerForCleaner.debug("Beginning log cleanup");
            int total = 0;
            final Timer.Context ctx = new Timer().time();
            for (final Log kafkaLog : JavaConversions.asJavaIterable(logManager.allLogs())) {
                if (kafkaLog.config().compact()) continue;
                loggerForCleaner.debug("Garbage collecting {}", kafkaLog.name());
                total += cleanupExpiredSegments(kafkaLog) +
                        cleanupSegmentsToMaintainSize(kafkaLog) +
                        cleanupSegmentsToRemoveCommitted(kafkaLog);
            }

            loggerForCleaner.debug("Log cleanup completed. {} files deleted in {} seconds",
                                   total,
                                   NANOSECONDS.toSeconds(ctx.stop()));
            return total;
        }

        private int cleanupExpiredSegments(final Log kafkaLog) {
            // don't run if nothing will be done
            if (kafkaLog.size() == 0 && kafkaLog.numberOfSegments() < 1) {
                return 0;
            }
            return kafkaLog.deleteOldSegments(new AbstractFunction1<LogSegment, Object>() {
                @Override
                public Object apply(LogSegment segment) {
                    final long segmentAge = JODA_TIME.milliseconds() - segment.lastModified();
                    final boolean shouldDelete = segmentAge > kafkaLog.config().retentionMs();
                    if (shouldDelete) {
                        loggerForCleaner.debug(
                                "[cleanup-time] Removing segment with age {}s, older than then maximum retention age {}s",
                                MILLISECONDS.toSeconds(segmentAge),
                                MILLISECONDS.toSeconds(kafkaLog.config().retentionMs()));
                    }
                    return shouldDelete;
                }
            });
        }

        private int cleanupSegmentsToMaintainSize(Log kafkaLog) {
            final long retentionSize = kafkaLog.config().retentionSize();
            if (retentionSize < 0 || kafkaLog.size() < retentionSize) {
                return 0;
            }
            final long[] diff = {kafkaLog.size() - retentionSize};
            return kafkaLog.deleteOldSegments(new AbstractFunction1<LogSegment, Object>() { // sigh scala
                @Override
                public Object apply(LogSegment segment) {
                    if (diff[0] - segment.size() >= 0) {
                        diff[0] -= segment.size();
                        loggerForCleaner.debug(
                                "[cleanup-size] Removing segment starting at offset {}, size {} bytes, to shrink log to new size {}, target size {}",
                                segment.baseOffset(),
                                segment.size(),
                                diff[0],
                                retentionSize);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        private int cleanupSegmentsToRemoveCommitted(Log kafkaLog) {
            if (kafkaLog.numberOfSegments() <= 1) {
                loggerForCleaner.debug(
                        "[cleanup-committed] The journal is already minimal at {} segment(s), not trying to remove more segments.",
                        kafkaLog.numberOfSegments());
                return 0;
            }
            // we need to iterate through all segments to the find the cutoff point for the committed offset.
            // unfortunately finding the largest offset contained in a segment is expensive (it involves reading the entire file)
            // so we have to get a global view.
            final long committedOffset = KafkaJournal.this.committedOffset.get();
            final HashSet<LogSegment> logSegments = Sets.newHashSet(
                    JavaConversions.asJavaIterable(kafkaLog.logSegments(committedOffset, Long.MAX_VALUE))
            );
            loggerForCleaner.debug("[cleanup-committed] Keeping segments {}", logSegments);
            return kafkaLog.deleteOldSegments(new AbstractFunction1<LogSegment, Object>() {
                @Override
                public Object apply(LogSegment segment) {
                    final boolean shouldDelete = !logSegments.contains(segment);
                    if (shouldDelete) {
                        loggerForCleaner.debug(
                                "[cleanup-committed] Should delete segment {} because it is prior to committed offset {}",
                                segment,
                                committedOffset);
                    }
                    return shouldDelete;
                }
            });
        }
    }

    public class RecoveryCheckpointFlusher implements Runnable {
        @Override
        public void run() {
            try {
                logManager.checkpointRecoveryPointOffsets();
            } catch (Exception e) {
                log.error("Unable to flush checkpoint recovery point offsets. Will try again.", e);
            }
        }
    }

    public class DirtyLogFlusher implements Runnable {
        @Override
        public void run() {
            try {
                flushDirtyLogs();
            } catch (Exception e) {
                log.error("Unable to flush dirty logs. Will try again.", e);
            }
        }
    }
}