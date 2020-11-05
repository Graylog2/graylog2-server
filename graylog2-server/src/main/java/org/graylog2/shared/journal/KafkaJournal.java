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
package org.graylog2.shared.journal;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Uninterruptibles;
import kafka.common.KafkaException;
import kafka.common.OffsetOutOfRangeException;
import kafka.common.TopicAndPartition;
import kafka.log.CleanerConfig;
import kafka.log.Log;
import kafka.log.LogAppendInfo;
import kafka.log.LogConfig;
import kafka.log.LogManager;
import kafka.log.LogSegment;
import kafka.message.ByteBufferMessageSet;
import kafka.message.Message;
import kafka.message.MessageAndOffset;
import kafka.message.MessageSet;
import kafka.server.BrokerState;
import kafka.server.RunningAsBroker;
import kafka.utils.KafkaScheduler;
import kafka.utils.Time;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.ThrottleState;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;
import org.graylog2.shared.metrics.HdrTimer;
import org.graylog2.shared.utilities.ByteBufferUtils;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Map$;
import scala.runtime.AbstractFunction1;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.io.UncheckedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.graylog2.plugin.Tools.bytesToHex;

@Singleton
public class KafkaJournal extends AbstractIdleService implements Journal {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaJournal.class);

    private static final int NUM_IO_THREADS = 1;

    public static final long DEFAULT_COMMITTED_OFFSET = Long.MIN_VALUE;
    public static final int NOTIFY_ON_UTILIZATION_PERCENTAGE = 95;
    public static final int THRESHOLD_THROTTLING_DISABLED = -1;

    // Metric names, which should be used twice (once in metric startup and once in metric teardown).
    public static final String METER_WRITTEN_MESSAGES = "writtenMessages";
    public static final String METER_READ_MESSAGES = "readMessages";
    private static final String METER_WRITE_DISCARDED_MESSAGES = "writeDiscardedMessages";
    public static final String GAUGE_UNCOMMITTED_MESSAGES = "uncommittedMessages";
    private static final String TIMER_WRITE_TIME = "writeTime";
    private static final String TIMER_READ_TIME = "readTime";
    private static final String METRIC_NAME_SIZE = "size";
    private static final String METRIC_NAME_LOG_END_OFFSET = "logEndOffset";
    private static final String METRIC_NAME_NUMBER_OF_SEGMENTS = "numberOfSegments";
    private static final String METRIC_NAME_UNFLUSHED_MESSAGES = "unflushedMessages";
    private static final String METRIC_NAME_RECOVERY_POINT = "recoveryPoint";
    private static final String METRIC_NAME_LAST_FLUSH_TIME = "lastFlushTime";

    // This exists so we can use JodaTime's millis provider in tests.
    // Kafka really only cares about the milliseconds() method in here.
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

    private final LogManager logManager;
    private final Log kafkaLog;
    private final File committedReadOffsetFile;
    private final AtomicLong committedOffset = new AtomicLong(DEFAULT_COMMITTED_OFFSET);
    private final MetricRegistry metricRegistry;
    private final ScheduledExecutorService scheduler;
    private final ServerStatus serverStatus;
    private final Timer writeTime;

    private final Timer readTime;
    private final KafkaScheduler kafkaScheduler;
    private final Meter writtenMessages;
    private final Meter readMessages;
    private final Meter writeDiscardedMessages;

    private final OffsetFileFlusher offsetFlusher;
    private final DirtyLogFlusher dirtyLogFlusher;
    private final RecoveryCheckpointFlusher recoveryCheckpointFlusher;
    private final LogRetentionCleaner logRetentionCleaner;
    private final long maxSegmentSize;
    private final int maxMessageSize;
    private final String metricPrefix;

    private long nextReadOffset = 0L;
    private ScheduledFuture<?> checkpointFlusherFuture;
    private ScheduledFuture<?> dirtyLogFlushFuture;
    private ScheduledFuture<?> logRetentionFuture;
    private ScheduledFuture<?> offsetFlusherFuture;
    private volatile boolean shuttingDown;
    private final AtomicReference<ThrottleState> throttleState = new AtomicReference<>();
    private final AtomicInteger purgedSegmentsInLastRetention = new AtomicInteger();

    private final int throttleThresholdPercentage;

    @Inject
    public KafkaJournal(@Named("message_journal_dir") Path journalDirectory,
                        @Named("scheduler") ScheduledExecutorService scheduler,
                        @Named("message_journal_segment_size") Size segmentSize,
                        @Named("message_journal_segment_age") Duration segmentAge,
                        @Named("message_journal_max_size") Size retentionSize,
                        @Named("message_journal_max_age") Duration retentionAge,
                        @Named("message_journal_flush_interval") long flushInterval,
                        @Named("message_journal_flush_age") Duration flushAge,
                        @Named("lb_throttle_threshold_percentage") int throttleThresholdPercentage,
                        MetricRegistry metricRegistry,
                        ServerStatus serverStatus) {

        this(journalDirectory, scheduler, segmentSize, segmentAge, retentionSize, retentionAge, flushInterval, flushAge,
             throttleThresholdPercentage, metricRegistry, serverStatus, KafkaJournal.class.getName());
    }

    /**
     * @param throttleThresholdPercentage The journal utilization percent at which throttling will be triggered.
     *                                    Expressed as an integer between 1 and 100. The value -1 disables throttling.
     */
    public KafkaJournal(Path journalDirectory,
                        ScheduledExecutorService scheduler,
                        Size segmentSize,
                        Duration segmentAge,
                        Size retentionSize,
                        Duration retentionAge,
                        long flushInterval,
                        Duration flushAge,
                        int throttleThresholdPercentage,
                        MetricRegistry metricRegistry,
                        ServerStatus serverStatus,
                        String metricPrefix) {

        // Only check throttleThresholdPercentage range if throttling is not disabled;
        if (throttleThresholdPercentage == THRESHOLD_THROTTLING_DISABLED) {
            this.throttleThresholdPercentage = throttleThresholdPercentage;
        } else {
            this.throttleThresholdPercentage = intRange(throttleThresholdPercentage, 0, 100);
        }

        this.scheduler = scheduler;
        this.serverStatus = serverStatus;
        this.maxSegmentSize = segmentSize.toBytes();
        // Max message size should not be bigger than max segment size.
        this.maxMessageSize = Ints.saturatedCast(maxSegmentSize);
        this.metricPrefix = metricPrefix;
        this.metricRegistry = metricRegistry;

        // Set up metrics
        this.writtenMessages = metricRegistry.meter(name(this.metricPrefix, METER_WRITTEN_MESSAGES));
        this.readMessages = metricRegistry.meter(name(this.metricPrefix, METER_READ_MESSAGES));
        this.writeDiscardedMessages = metricRegistry.meter(name(this.metricPrefix, METER_WRITE_DISCARDED_MESSAGES));
        registerUncommittedGauge(metricRegistry, name(this.metricPrefix, GAUGE_UNCOMMITTED_MESSAGES));
        this.writeTime = registerHdrTimer(metricRegistry, name(this.metricPrefix, TIMER_WRITE_TIME)); // the registerHdrTimer helper doesn't throw on existing metrics
        this.readTime = registerHdrTimer(metricRegistry, name(this.metricPrefix, TIMER_READ_TIME)); // the registerHdrTimer helper doesn't throw on existing metrics

        final Map<String, Object> config = ImmutableMap.<String, Object>builder()
                // segmentSize: The soft maximum for the size of a segment file in the log
                .put(LogConfig.SegmentBytesProp(), Ints.saturatedCast(segmentSize.toBytes()))
                // segmentMs: The soft maximum on the amount of time before a new log segment is rolled
                .put(LogConfig.SegmentMsProp(), segmentAge.getMillis())
                // segmentJitterMs The maximum random jitter subtracted from segmentMs to avoid thundering herds of segment rolling
                .put(LogConfig.SegmentJitterMsProp(), 0)
                // flushInterval: The number of messages that can be written to the log before a flush is forced
                .put(LogConfig.FlushMessagesProp(), flushInterval)
                // flushMs: The amount of time the log can have dirty data before a flush is forced
                .put(LogConfig.FlushMsProp(), flushAge.getMillis())
                // retentionSize: The approximate total number of bytes this log can use
                .put(LogConfig.RetentionBytesProp(), retentionSize.toBytes())
                // retentionMs: The age approximate maximum age of the last segment that is retained
                .put(LogConfig.RetentionMsProp(), retentionAge.getMillis())
                // maxMessageSize: The maximum size of a message in the log (ensure that it's not larger than the max segment size)
                .put(LogConfig.MaxMessageBytesProp(), maxMessageSize)
                // maxIndexSize: The maximum size of an index file
                .put(LogConfig.SegmentIndexBytesProp(), Ints.saturatedCast(Size.megabytes(1L).toBytes()))
                // indexInterval: The approximate number of bytes between index entries
                .put(LogConfig.IndexIntervalBytesProp(), 4096)
                // fileDeleteDelayMs: The time to wait before deleting a file from the filesystem
                .put(LogConfig.FileDeleteDelayMsProp(), MINUTES.toMillis(1L))
                // deleteRetentionMs: The time to retain delete markers in the log. Only applicable for logs that are being compacted.
                .put(LogConfig.DeleteRetentionMsProp(), DAYS.toMillis(1L))
                // minCleanableRatio: The ratio of bytes that are available for cleaning to the bytes already cleaned
                .put(LogConfig.MinCleanableDirtyRatioProp(), 0.5)
                // compact: Should old segments in this log be deleted or de-duplicated?
                .put(LogConfig.Compact(), false)
                // uncleanLeaderElectionEnable Indicates whether unclean leader election is enabled; actually a controller-level property
                //                             but included here for topic-specific configuration validation purposes
                .put(LogConfig.UncleanLeaderElectionEnableProp(), true)
                // minInSyncReplicas If number of insync replicas drops below this number, we stop accepting writes with -1 (or all) required acks
                .put(LogConfig.MinInSyncReplicasProp(), 1)
                .build();
        final LogConfig defaultConfig = new LogConfig(config);

        // these are the default values as per kafka 0.8.1.1, except we don't turn on the cleaner
        // Cleaner really is log compaction with respect to "deletes" in the log.
        // we never insert a message twice, at least not on purpose, so we do not "clean" logs, ever.
        final CleanerConfig cleanerConfig =
                new CleanerConfig(
                        1,
                        Size.megabytes(4L).toBytes(),
                        0.9d,
                        Ints.saturatedCast(Size.megabytes(1L).toBytes()),
                        Ints.saturatedCast(Size.megabytes(32L).toBytes()),
                        Ints.saturatedCast(Size.megabytes(5L).toBytes()),
                        SECONDS.toMillis(15L),
                        false,
                        "MD5");

        if (!java.nio.file.Files.exists(journalDirectory)) {
            try {
                java.nio.file.Files.createDirectories(journalDirectory);
            } catch (IOException e) {
                LOG.error("Cannot create journal directory at {}, please check the permissions", journalDirectory.toAbsolutePath());
                throw new UncheckedIOException(e);
            }
        }

        // TODO add check for directory, etc
        committedReadOffsetFile = new File(journalDirectory.toFile(), "graylog2-committed-read-offset");
        try {
            if (!committedReadOffsetFile.createNewFile()) {
                final String line = Files.asCharSource(committedReadOffsetFile, StandardCharsets.UTF_8).readFirstLine();
                // the file contains the last offset graylog2 has successfully processed.
                // thus the nextReadOffset is one beyond that number
                if (line != null) {
                    committedOffset.set(Long.parseLong(line.trim()));
                    nextReadOffset = committedOffset.get() + 1;
                }
            }
        } catch (IOException e) {
            LOG.error("Cannot access offset file: {}", e.getMessage());
            final AccessDeniedException accessDeniedException = new AccessDeniedException(committedReadOffsetFile.getAbsolutePath(), null, e.getMessage());
            throw new RuntimeException(accessDeniedException);
        }
        try {
            final BrokerState brokerState = new BrokerState();
            brokerState.newState(RunningAsBroker.state());
            kafkaScheduler = new KafkaScheduler(2, "kafka-journal-scheduler-", false); // TODO make thread count configurable
            kafkaScheduler.startup();
            logManager = new LogManager(
                    new File[]{journalDirectory.toFile()},
                    Map$.MODULE$.<String, LogConfig>empty(),
                    defaultConfig,
                    cleanerConfig,
                    NUM_IO_THREADS,
                    SECONDS.toMillis(60L),
                    SECONDS.toMillis(60L),
                    SECONDS.toMillis(60L),
                    kafkaScheduler, // Broker state
                    brokerState,
                    JODA_TIME);

            final TopicAndPartition topicAndPartition = new TopicAndPartition("messagejournal", 0);
            final Option<Log> messageLog = logManager.getLog(topicAndPartition);
            if (messageLog.isEmpty()) {
                kafkaLog = logManager.createLog(topicAndPartition, logManager.defaultConfig());
            } else {
                kafkaLog = messageLog.get();
            }

            // Set up more metrics
            this.metricRegistry.register(name(metricPrefix, METRIC_NAME_SIZE), (Gauge<Long>) kafkaLog::size);
            this.metricRegistry.register(name(metricPrefix, METRIC_NAME_LOG_END_OFFSET), (Gauge<Long>) kafkaLog::logEndOffset);
            this.metricRegistry.register(name(metricPrefix, METRIC_NAME_NUMBER_OF_SEGMENTS), (Gauge<Integer>) kafkaLog::numberOfSegments);
            this.metricRegistry.register(name(metricPrefix, METRIC_NAME_UNFLUSHED_MESSAGES), (Gauge<Long>) kafkaLog::unflushedMessages);
            this.metricRegistry.register(name(metricPrefix, METRIC_NAME_RECOVERY_POINT), (Gauge<Long>) kafkaLog::recoveryPoint);
            this.metricRegistry.register(name(metricPrefix, METRIC_NAME_LAST_FLUSH_TIME), (Gauge<Long>) kafkaLog::lastFlushTime);

            // must not be a lambda, because the serialization cannot determine the proper Metric type :(
            this.metricRegistry.register(getOldestSegmentMetricName(), (Gauge<Date>) new Gauge<Date>() {
                @Override
                public Date getValue() {
                    long oldestSegment = Long.MAX_VALUE;
                    for (final LogSegment segment : KafkaJournal.this.getSegments()) {
                        oldestSegment = Math.min(oldestSegment, segment.created());
                    }

                    return new Date(oldestSegment);
                }
            });

            LOG.info("Initialized Kafka based journal at {}", journalDirectory);

            offsetFlusher = new OffsetFileFlusher();
            dirtyLogFlusher = new DirtyLogFlusher();
            recoveryCheckpointFlusher = new RecoveryCheckpointFlusher();
            logRetentionCleaner = new LogRetentionCleaner();
        } catch (KafkaException e) {
            // most likely failed to grab lock
            LOG.error("Unable to start logmanager.", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Ensures that an integer is within a given range.
     *
     * @param i   The number to check.
     * @param min The minimum value to return.
     * @param max The maximum value to return.
     * @return {@code i} if the number is between {@code min} and {@code max},
     *         {@code min} if {@code i} is less than the minimum,
     *         {@code max} if {@code i} is greater than the maximum.
     */
    private static int intRange(int i, int min, int max) {
        return Integer.min(Integer.max(min, i), max);
    }

    private Timer registerHdrTimer(MetricRegistry metricRegistry, final String metricName) {
        Timer timer;
        try {
            timer = metricRegistry.register(metricName, new HdrTimer(1, MINUTES, 1));
        } catch (IllegalArgumentException e) {
            final SortedMap<String, Timer> timers = metricRegistry.getTimers((name, metric) -> metricName.equals(name));
            timer = Iterables.getOnlyElement(timers.values());
        }
        return timer;
    }

    private void registerUncommittedGauge(MetricRegistry metricRegistry, String name) {
        try {
            metricRegistry.register(name,
                    (Gauge<Long>) () -> {
                        if (getCommittedOffset() == DEFAULT_COMMITTED_OFFSET && size() == 0) {
                            // nothing committed at all
                            return 0L;
                        }
                        return Math.max(0, getLogEndOffset() - 1 - committedOffset.get());
                    });
        } catch (IllegalArgumentException ignored) {
            // already registered, we'll ignore that.
        }
    }

    public int getPurgedSegmentsInLastRetention() {
        return purgedSegmentsInLastRetention.get();
    }

    /**
     * Call this at journal shutdown time. This removes all metrics, so they can be re-registered when startup occurs
     * again in the future. This is particularly used for code that creates multiple journal instances that can be
     * restarted independently (eg. the Forwarder).
     */
    private void teardownLogMetrics() {

        this.metricRegistry.remove(name(metricPrefix, METER_WRITTEN_MESSAGES));
        this.metricRegistry.remove(name(metricPrefix, METER_READ_MESSAGES));
        this.metricRegistry.remove(name(metricPrefix, METER_WRITE_DISCARDED_MESSAGES));
        this.metricRegistry.remove(name(metricPrefix, GAUGE_UNCOMMITTED_MESSAGES));
        this.metricRegistry.remove(name(metricPrefix, TIMER_WRITE_TIME));
        this.metricRegistry.remove(name(metricPrefix, TIMER_READ_TIME));
        this.metricRegistry.remove(name(metricPrefix, METRIC_NAME_SIZE));
        this.metricRegistry.remove(name(metricPrefix, METRIC_NAME_LOG_END_OFFSET));
        this.metricRegistry.remove(name(metricPrefix, METRIC_NAME_NUMBER_OF_SEGMENTS));
        this.metricRegistry.remove(name(metricPrefix, METRIC_NAME_UNFLUSHED_MESSAGES));
        this.metricRegistry.remove(name(metricPrefix, METRIC_NAME_RECOVERY_POINT));
        this.metricRegistry.remove(name(metricPrefix, METRIC_NAME_LAST_FLUSH_TIME));
        this.metricRegistry.remove(getOldestSegmentMetricName());
    }

    private String getOldestSegmentMetricName() {

        /* Hack for backwards comparability: JOURNAL_OLDEST_SEGMENT is a global metric name (and displayed on the
         * Journal page for the input journal). The Forwarder outputs use a unique metric name since it create instances
         * of KafkaJournal. This is an override to provide the old metric name for the input journal, and unique names
         * for instances. */
        String journalOldestSegmentMetricName = GlobalMetricNames.JOURNAL_OLDEST_SEGMENT;
        if (!KafkaJournal.class.getName().equals(metricPrefix)) {
            journalOldestSegmentMetricName = name(metricPrefix, GlobalMetricNames.OLDEST_SEGMENT_SUFFIX);
        }
        return journalOldestSegmentMetricName;
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
            long messageSetSize = 0L;
            long lastWriteOffset = 0L;

            final List<Message> messages = new ArrayList<>(entries.size());
            for (final Entry entry : entries) {
                final byte[] messageBytes = entry.getMessageBytes();
                final byte[] idBytes = entry.getIdBytes();

                payloadSize += messageBytes.length;

                final Message newMessage = new Message(messageBytes, idBytes);
                // Calculate the size of the new message in the message set by including the overhead for the log entry.
                final int newMessageSize = MessageSet.entrySize(newMessage);

                if (newMessageSize > maxMessageSize) {
                    writeDiscardedMessages.mark();
                    LOG.warn("Message with ID <{}> is too large to store in journal, skipping! (size: {} bytes / max: {} bytes)",
                            new String(idBytes, StandardCharsets.UTF_8), newMessageSize, maxMessageSize);
                    payloadSize = 0;
                    continue;
                }

                // If adding the new message to the message set would overflow the max segment size, flush the current
                // list of message to avoid a MessageSetSizeTooLargeException.
                if ((messageSetSize + newMessageSize) > maxSegmentSize) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Flushing {} bytes message set with {} messages to avoid overflowing segment with max size of {} bytes",
                                messageSetSize, messages.size(), maxSegmentSize);
                    }
                    lastWriteOffset = flushMessages(messages, payloadSize);
                    // Reset the messages list and size counters to start a new batch.
                    messages.clear();
                    messageSetSize = 0;
                    payloadSize = 0;
                }
                messages.add(newMessage);
                messageSetSize += newMessageSize;

                if (LOG.isTraceEnabled()) {
                    LOG.trace("Message {} contains bytes {}", bytesToHex(idBytes), bytesToHex(messageBytes));
                }
            }

            // Flush the rest of the messages.
            if (messages.size() > 0) {
                lastWriteOffset = flushMessages(messages, payloadSize);
            }

            return lastWriteOffset;
        }
    }

    private long flushMessages(List<Message> messages, long payloadSize) {
        if (messages.isEmpty()) {
            LOG.debug("No messages to flush, not trying to write an empty message set.");
            return -1L;
        }

        final ByteBufferMessageSet messageSet = new ByteBufferMessageSet(JavaConversions.asScalaBuffer(messages).toSeq());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Trying to write ByteBufferMessageSet with size of {} bytes to journal", messageSet.sizeInBytes());
        }

        final LogAppendInfo appendInfo = kafkaLog.append(messageSet, true);
        long lastWriteOffset = appendInfo.lastOffset();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Wrote {} messages to journal: {} bytes (payload {} bytes), log position {} to {}",
                    messages.size(), messageSet.sizeInBytes(), payloadSize, appendInfo.firstOffset(), lastWriteOffset);
        }
        writtenMessages.mark(messages.size());

        return lastWriteOffset;
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
        return readNext(nextReadOffset, requestedMaximumCount);
    }

    /**
     * Read next messages from the journal, starting at the given offset. If the underlying journal implementation
     * returns an empty list of entries, but we know there are more entries in the journal, we'll try to skip the
     * problematic offset(s) until we find entries again.
     *
     * @param startOffset Offset to start reading at
     * @param requestedMaximumCount Maximum number of entries to return.
     * @return A list of entries
     */
    public List<JournalReadEntry> readNext(long startOffset, long requestedMaximumCount) {
        List<JournalReadEntry> messages = read(startOffset, requestedMaximumCount);

        if (messages.isEmpty()) {
            // If we got an empty result BUT we know that there are more messages in the log, we bump the readOffset
            // by 1 and try to read again. We continue until we either get an non-empty result or we reached the
            // end of the log.
            // This can happen when a log segment is truncated at the end but later segments have valid messages again.
            long failedReadOffset = startOffset;
            long retryReadOffset = failedReadOffset + 1;

            while (messages.isEmpty() && failedReadOffset < (getLogEndOffset() - 1)) {
                LOG.warn(
                        "Couldn't read any messages from offset <{}> but journal has more messages. Skipping and " +
                                "trying to read from offset <{}>",
                        failedReadOffset, retryReadOffset);

                // Retry the read with an increased offset to skip corrupt segments
                messages = read(retryReadOffset, requestedMaximumCount);

                // Bump offsets in case we still read an empty result
                failedReadOffset++;
                retryReadOffset++;
            }
        }

        return messages;
    }

    /**
     * Read from the journal, starting at the given offset. If the underlying journal implementation returns an empty
     * list of entries, it will be returned even if we know there are more entries in the journal.
     *
     * @param readOffset Offset to start reading at
     * @param requestedMaximumCount Maximum number of entries to return.
     * @return A list of entries
     */
    public List<JournalReadEntry> read(long readOffset, long requestedMaximumCount) {
        // Always read at least one!
        final long maximumCount = Math.max(1, requestedMaximumCount);
        long maxOffset = readOffset + maximumCount;

        if (shuttingDown) {
            return Collections.emptyList();
        }
        final List<JournalReadEntry> messages = new ArrayList<>(Ints.saturatedCast(maximumCount));
        try (Timer.Context ignored = readTime.time()) {
            final long logStartOffset = getLogStartOffset();

            if (readOffset < logStartOffset) {
                LOG.info(
                        "Read offset {} before start of log at {}, starting to read from the beginning of the journal.",
                        readOffset,
                        logStartOffset);
                readOffset = logStartOffset;
                maxOffset = readOffset + maximumCount;
            }
            LOG.debug("Requesting to read a maximum of {} messages (or 5MB) from the journal, offset interval [{}, {})",
                    maximumCount, readOffset, maxOffset);

            // TODO benchmark and make read-ahead strategy configurable for performance tuning
            final MessageSet messageSet = kafkaLog.read(readOffset,
                    5 * 1024 * 1024,
                    Option.<Object>apply(maxOffset)).messageSet();

            final Iterator<MessageAndOffset> iterator = messageSet.iterator();
            long firstOffset = Long.MIN_VALUE;
            long lastOffset = Long.MIN_VALUE;
            long totalBytes = 0;
            while (iterator.hasNext()) {
                final MessageAndOffset messageAndOffset = iterator.next();

                if (firstOffset == Long.MIN_VALUE) firstOffset = messageAndOffset.offset();
                // always remember the last seen offset for debug purposes below
                lastOffset = messageAndOffset.offset();

                final byte[] payloadBytes = ByteBufferUtils.readBytes(messageAndOffset.message().payload());
                if (LOG.isTraceEnabled()) {
                    final byte[] keyBytes = ByteBufferUtils.readBytes(messageAndOffset.message().key());
                    LOG.trace("Read message {} contains {}", bytesToHex(keyBytes), bytesToHex(payloadBytes));
                }
                totalBytes += payloadBytes.length;
                messages.add(new JournalReadEntry(payloadBytes, messageAndOffset.offset()));
                // remember where to read from
                nextReadOffset = messageAndOffset.nextOffset();
            }
            if (messages.isEmpty()) {
                LOG.debug("No messages available to read for offset interval [{}, {}).", readOffset, maxOffset);
            } else {
                LOG.debug(
                        "Read {} messages, total payload size {}, from journal, offset interval [{}, {}], requested read at {}",
                        messages.size(),
                        totalBytes,
                        firstOffset,
                        lastOffset,
                        readOffset);
            }

        } catch (OffsetOutOfRangeException e) {
            // This is fine, the reader tries to read faster than the writer committed data. Next read will get the data.
            LOG.debug("Offset out of range, no messages available starting at offset {}", readOffset);
        } catch (Exception e) {
            // the scala code does not declare the IOException in kafkaLog.read() so we can't catch it here
            // sigh.
            if (shuttingDown) {
                LOG.debug("Caught exception during shutdown, ignoring it because we might have been blocked on a read.");
                return Collections.emptyList();
            }
            //noinspection ConstantConditions
            if (e instanceof ClosedByInterruptException) {
                LOG.debug("Interrupted while reading from journal, during shutdown this is harmless and ignored.", e);
            } else {
                throw e;
            }

        }
        readMessages.mark(messages.size());
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
                LOG.warn("Committing journal offset spins {} times now, this might be a bug. Continuing to try update.",
                        i);
            }
        } while (!committedOffset.compareAndSet(prev, Math.max(offset, prev)));

    }

    /**
     * A Java transliteration of what the scala implementation does, which unfortunately is declared as private
     */
    protected void flushDirtyLogs() {
        LOG.debug("Checking for dirty logs to flush...");

        final Set<Map.Entry<TopicAndPartition, Log>> entries = JavaConversions.mapAsJavaMap(logManager.logsByTopicPartition()).entrySet();
        for (final Map.Entry<TopicAndPartition, Log> topicAndPartitionLogEntry : entries) {
            final TopicAndPartition topicAndPartition = topicAndPartitionLogEntry.getKey();
            final Log kafkaLog = topicAndPartitionLogEntry.getValue();
            final long timeSinceLastFlush = JODA_TIME.milliseconds() - kafkaLog.lastFlushTime();
            try {
                LOG.debug(
                        "Checking if flush is needed on {} flush interval {} last flushed {} time since last flush: {}",
                        topicAndPartition.topic(),
                        kafkaLog.config().flushInterval(),
                        kafkaLog.lastFlushTime(),
                        timeSinceLastFlush);
                if (timeSinceLastFlush >= kafkaLog.config().flushMs()) {
                    kafkaLog.flush();
                }
            } catch (Exception e) {
                LOG.error("Error flushing topic " + topicAndPartition.topic(), e);
            }
        }
    }

    public long getCommittedOffset() {
        return committedOffset.get();
    }

    public long getNextReadOffset() {
        return nextReadOffset;
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
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.debug("Shutting down journal!");
        shuttingDown = true;

        offsetFlusherFuture.cancel(false);
        logRetentionFuture.cancel(false);
        checkpointFlusherFuture.cancel(false);
        dirtyLogFlushFuture.cancel(false);

        kafkaScheduler.shutdown();
        logManager.shutdown();
        // final flush
        offsetFlusher.run();

        // Teardown log metrics to prevent errors when restarting instances.
        teardownLogMetrics();
    }

    public int cleanupLogs() {
        try {
            return logRetentionCleaner.call();
        } catch (Exception e) {
            LOG.error("Unable to delete expired segments.", e);
            return 0;
        }
    }

    // default visibility for tests
    public Iterable<LogSegment> getSegments() {
        return JavaConversions.asJavaIterable(kafkaLog.logSegments());
    }

    /**
     * Returns the journal size in bytes, exluding index files.
     *
     * @return journal size in bytes
     */
    public long size() {
        return kafkaLog.size();
    }

    /**
     * Returns the number of segments this journal consists of.
     *
     * @return number of segments
     */
    public int numberOfSegments() {
        return kafkaLog.numberOfSegments();
    }

    /**
     * Returns the highest journal offset that has been writting to persistent storage by Graylog.
     * <p>
     * Every message at an offset prior to this one can be considered as processed and does not need to be held in
     * the journal any longer. By default Graylog will try to aggressively flush the journal to consume a smaller
     * amount of disk space.
     * </p>
     *
     * @return the offset of the last message which has been successfully processed.
     */
    public long getCommittedReadOffset() {
        return committedOffset.get();
    }

    /**
     * Discards all data in the journal prior to the given offset.
     *
     * @param offset offset to truncate to, so that no offset in the journal is larger than this.
     */
    public void truncateTo(long offset) {
        kafkaLog.truncateTo(offset);
    }

    /**
     * Returns the first valid offset in the entire journal.
     *
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
     *
     * @return the next offset value (last valid offset is this number - 1)
     */
    public long getLogEndOffset() {
        return kafkaLog.logEndOffset();
    }

    /**
     * For informational purposes this method provides access to the current state of the journal.
     *
     * @return the journal state for throttling purposes
     */
    public ThrottleState getThrottleState() {
        return throttleState.get();
    }

    public void setThrottleState(ThrottleState state) {
        throttleState.set(state);
    }


    public class OffsetFileFlusher implements Runnable {
        @Override
        public void run() {
            // Do not write the file if committedOffset has never been updated.
            if (committedOffset.get() == DEFAULT_COMMITTED_OFFSET) {
                return;
            }
            try (final FileOutputStream fos = new FileOutputStream(committedReadOffsetFile)) {
                fos.write(String.valueOf(committedOffset.get()).getBytes(StandardCharsets.UTF_8));
                // flush stream
                fos.flush();
                // actually sync to disk
                fos.getFD().sync();
            } catch (SyncFailedException e) {
                LOG.error("Cannot sync " + committedReadOffsetFile.getAbsolutePath() + " to disk. Continuing anyway," +
                        " but there is no guarantee that the file has been written.", e);
            } catch (IOException e) {
                LOG.error("Cannot write " + committedReadOffsetFile.getAbsolutePath() + " to disk.", e);
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
                KafkaJournal.this.purgedSegmentsInLastRetention.set(0);
                return 0;
            }
            int deletedSegments = kafkaLog.deleteOldSegments(new AbstractFunction1<LogSegment, Object>() {
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
            KafkaJournal.this.purgedSegmentsInLastRetention.set(deletedSegments);
            return deletedSegments;
        }

        /**
         * Change the load balancer status from ALIVE to THROTTLE, or vice versa depending on the
         * journal utilization percentage. As the utilization ratio is reliable only after cleanup,
         * that's where this is called from.
         */
        private void updateLoadBalancerStatus(double utilizationPercentage) {
            final LoadBalancerStatus currentStatus = serverStatus.getLifecycle().getLoadbalancerStatus();

            // Flip the status. The next lifecycle events may change status. This should be good enough, because
            // throttling does not offer hard guarantees.
            if (currentStatus == LoadBalancerStatus.THROTTLED && utilizationPercentage < throttleThresholdPercentage) {
                serverStatus.running();
                LOG.info(String.format(Locale.ENGLISH,
                    "Journal usage is %.2f%% (threshold %d%%), changing load balancer status from THROTTLED to ALIVE",
                    utilizationPercentage, throttleThresholdPercentage));
            } else if (currentStatus == LoadBalancerStatus.ALIVE && utilizationPercentage >= throttleThresholdPercentage) {
                serverStatus.throttle();
                LOG.info(String.format(Locale.ENGLISH,
                    "Journal usage is %.2f%% (threshold %d%%), changing load balancer status from ALIVE to THROTTLED",
                    utilizationPercentage, throttleThresholdPercentage));
            }
        }

        private int cleanupSegmentsToMaintainSize(Log kafkaLog) {
            final long retentionSize = kafkaLog.config().retentionSize();
            final long currentSize = kafkaLog.size();
            final double utilizationPercentage = retentionSize > 0 ? (currentSize * 100) / retentionSize : 0.0;
            if (utilizationPercentage > KafkaJournal.NOTIFY_ON_UTILIZATION_PERCENTAGE) {
                LOG.warn("Journal utilization ({}%) has gone over {}%.", utilizationPercentage,
                        KafkaJournal.NOTIFY_ON_UTILIZATION_PERCENTAGE);
            }

            // Don't update the load balancer state if throttling is disabled.
            if (throttleThresholdPercentage != THRESHOLD_THROTTLING_DISABLED) {
                updateLoadBalancerStatus(utilizationPercentage);
            }

            if (retentionSize < 0 || currentSize < retentionSize) {
                KafkaJournal.this.purgedSegmentsInLastRetention.set(0);
                return 0;
            }
            final long[] diff = {currentSize - retentionSize};
            int deletedSegments = kafkaLog.deleteOldSegments(new AbstractFunction1<LogSegment, Object>() { // sigh scala
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
            KafkaJournal.this.purgedSegmentsInLastRetention.set(deletedSegments);
            return deletedSegments;
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
                LOG.error("Unable to flush checkpoint recovery point offsets. Will try again.", e);
            }
        }
    }

    public class DirtyLogFlusher implements Runnable {
        @Override
        public void run() {
            try {
                flushDirtyLogs();
            } catch (Exception e) {
                LOG.error("Unable to flush dirty logs. Will try again.", e);
            }
        }
    }
}
