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
package org.graylog2.shared.journal;

import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import kafka.common.KafkaException;
import kafka.log.LogSegment;
import kafka.utils.FileLock;
import org.graylog2.plugin.InstantMillisProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.fileFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class KafkaJournalTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ScheduledThreadPoolExecutor scheduler;
    private File journalDirectory;

    @Before
    public void setUp() throws IOException {
        scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.prestartCoreThread();
        journalDirectory = temporaryFolder.newFolder();
    }

    @After
    public void tearDown() {
        scheduler.shutdown();
    }

    @Test
    public void writeAndRead() throws IOException {
        final Journal journal = new KafkaJournal(journalDirectory,
                scheduler,
                Size.megabytes(100L),
                Duration.standardHours(1),
                Size.megabytes(5L),
                Duration.standardHours(1),
                1_000_000,
                Duration.standardMinutes(1),
                new MetricRegistry());

        final byte[] idBytes = "id".getBytes(UTF_8);
        final byte[] messageBytes = "message".getBytes(UTF_8);

        final long position = journal.write(idBytes, messageBytes);
        final List<Journal.JournalReadEntry> messages = journal.read(1);

        final Journal.JournalReadEntry firstMessage = Iterators.getOnlyElement(messages.iterator());

        assertEquals("message", new String(firstMessage.getPayload(), UTF_8));
    }

    @Test
    public void readAtLeastOne() throws Exception {
        final Journal journal = new KafkaJournal(journalDirectory,
                scheduler,
                Size.megabytes(100L),
                Duration.standardHours(1),
                Size.megabytes(5L),
                Duration.standardHours(1),
                1_000_000,
                Duration.standardMinutes(1),
                new MetricRegistry());

        final byte[] idBytes = "id".getBytes(UTF_8);
        final byte[] messageBytes = "message1".getBytes(UTF_8);

        final long position = journal.write(idBytes, messageBytes);

        // Trying to read 0 should always read at least 1 entry.
        final List<Journal.JournalReadEntry> messages = journal.read(0);

        final Journal.JournalReadEntry firstMessage = Iterators.getOnlyElement(messages.iterator());

        assertEquals("message1", new String(firstMessage.getPayload(), UTF_8));
    }

    private int createBulkChunks(KafkaJournal journal, Size segmentSize, int bulkCount) {
        // Magic numbers deduced by magic…
        int bulkSize = Ints.saturatedCast(segmentSize.toBytes() / (2L * 16L));
        // perform multiple writes to make multiple segments
        for (int currentBulk = 0; currentBulk < bulkCount; currentBulk++) {
            final List<Journal.Entry> entries = Lists.newArrayListWithExpectedSize(bulkSize);
            long writtenBytes = 0L;
            for (int i = 0; i < bulkSize; i++) {
                final byte[] idBytes = ("id" + i).getBytes(UTF_8);
                final byte[] messageBytes = ("message " + i).getBytes(UTF_8);

                writtenBytes += 3 * (idBytes.length + messageBytes.length);
                if (writtenBytes > segmentSize.toBytes()) {
                    break;
                }
                entries.add(journal.createEntry(idBytes, messageBytes));
            }
            journal.write(entries);
        }

        return bulkSize;
    }

    private int countSegmentsInDir(File messageJournalFile) {
        // let it throw
        return messageJournalFile.list(and(fileFileFilter(), suffixFileFilter(".log"))).length;
    }

    @Test
    public void segmentRotation() throws Exception {
        final Size segmentSize = Size.kilobytes(1L);
        final KafkaJournal journal = new KafkaJournal(journalDirectory,
                scheduler,
                segmentSize,
                Duration.standardHours(1),
                Size.kilobytes(10L),
                Duration.standardDays(1),
                1_000_000,
                Duration.standardMinutes(1),
                new MetricRegistry());

        createBulkChunks(journal, segmentSize, 3);

        final File[] files = journalDirectory.listFiles();
        assertNotNull(files);
        assertTrue("there should be files in the journal directory", files.length > 0);

        final File[] messageJournalDir = journalDirectory.listFiles((FileFilter) and(directoryFileFilter(),
                nameFileFilter("messagejournal-0")));
        assertTrue(messageJournalDir.length == 1);
        final File[] logFiles = messageJournalDir[0].listFiles((FileFilter) and(fileFileFilter(),
                suffixFileFilter(".log")));
        assertEquals("should have two journal segments", 3, logFiles.length);
    }

    @Test
    public void segmentSizeCleanup() throws Exception {
        final Size segmentSize = Size.kilobytes(1L);
        final KafkaJournal journal = new KafkaJournal(journalDirectory,
                scheduler,
                segmentSize,
                Duration.standardHours(1),
                Size.kilobytes(1L),
                Duration.standardDays(1),
                1_000_000,
                Duration.standardMinutes(1),
                new MetricRegistry());
        final File messageJournalDir = new File(journalDirectory, "messagejournal-0");
        assertTrue(messageJournalDir.exists());

        // create enough chunks so that we exceed the maximum journal size we configured
        createBulkChunks(journal, segmentSize, 3);

        // make sure everything is on disk
        journal.flushDirtyLogs();

        assertEquals(3, countSegmentsInDir(messageJournalDir));

        final int cleanedLogs = journal.cleanupLogs();
        assertEquals(1, cleanedLogs);

        final int numberOfSegments = countSegmentsInDir(messageJournalDir);
        assertEquals(2, numberOfSegments);
    }

    @Test
    public void segmentAgeCleanup() throws Exception {
        final InstantMillisProvider clock = new InstantMillisProvider(DateTime.now(DateTimeZone.UTC));

        DateTimeUtils.setCurrentMillisProvider(clock);
        try {
            final Size segmentSize = Size.kilobytes(1L);
            final KafkaJournal journal = new KafkaJournal(journalDirectory,
                    scheduler,
                    segmentSize,
                    Duration.standardHours(1),
                    Size.kilobytes(10L),
                    Duration.standardMinutes(1),
                    1_000_000,
                    Duration.standardMinutes(1),
                    new MetricRegistry());
            final File messageJournalDir = new File(journalDirectory, "messagejournal-0");
            assertTrue(messageJournalDir.exists());

            // we need to fix up the last modified times of the actual files.
            long lastModifiedTs[] = new long[2];

            // create two chunks, 30 seconds apart
            createBulkChunks(journal, segmentSize, 1);
            journal.flushDirtyLogs();
            lastModifiedTs[0] = clock.getMillis();

            clock.tick(Period.seconds(30));

            createBulkChunks(journal, segmentSize, 1);
            journal.flushDirtyLogs();
            lastModifiedTs[1] = clock.getMillis();

            int i = 0;
            for (final LogSegment segment : journal.getSegments()) {
                assertTrue(i < 2);
                segment.lastModified_$eq(lastModifiedTs[i]);
                i++;
            }

            int cleanedLogs = journal.cleanupLogs();
            assertEquals("no segments should've been cleaned", cleanedLogs, 0);
            assertEquals("two segments segment should remain", countSegmentsInDir(messageJournalDir), 2);

            // move clock beyond the retention period and clean again
            clock.tick(Period.seconds(120));

            cleanedLogs = journal.cleanupLogs();
            assertEquals("two segments should've been cleaned (only one will actually be removed...)", cleanedLogs, 2);
            assertEquals("one segment should remain", countSegmentsInDir(messageJournalDir), 1);

        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    @Test
    public void segmentCommittedCleanup() throws Exception {
        final Size segmentSize = Size.kilobytes(1L);
        final KafkaJournal journal = new KafkaJournal(journalDirectory,
                scheduler,
                segmentSize,
                Duration.standardHours(1),
                Size.petabytes(1L), // never clean by size in this test
                Duration.standardDays(1),
                1_000_000,
                Duration.standardMinutes(1),
                new MetricRegistry());
        final File messageJournalDir = new File(journalDirectory, "messagejournal-0");
        assertTrue(messageJournalDir.exists());

        final int bulkSize = createBulkChunks(journal, segmentSize, 3);

        // make sure everything is on disk
        journal.flushDirtyLogs();

        assertEquals(countSegmentsInDir(messageJournalDir), 3);

        // we haven't committed any offsets, this should not touch anything.
        final int cleanedLogs = journal.cleanupLogs();
        assertEquals(cleanedLogs, 0);

        final int numberOfSegments = countSegmentsInDir(messageJournalDir);
        assertEquals(numberOfSegments, 3);

        // mark first half of first segment committed, should not clean anything
        journal.markJournalOffsetCommitted(bulkSize / 2);
        assertEquals("should not touch segments", journal.cleanupLogs(), 0);
        assertEquals(countSegmentsInDir(messageJournalDir), 3);

        journal.markJournalOffsetCommitted(bulkSize + 1);
        assertEquals("first segment should've been purged", journal.cleanupLogs(), 1);
        assertEquals(countSegmentsInDir(messageJournalDir), 2);

        journal.markJournalOffsetCommitted(bulkSize * 4);
        assertEquals("only purge one segment, not the active one", journal.cleanupLogs(), 1);
        assertEquals(countSegmentsInDir(messageJournalDir), 1);
    }

    @Test
    public void lockedJournalDir() throws Exception {
        // Grab the lock before starting the KafkaJournal.
        final File file = new File(journalDirectory, ".lock");
        assumeTrue(file.createNewFile());
        final FileLock fileLock = new FileLock(file);
        assumeTrue(fileLock.tryLock());

        try {
            new KafkaJournal(journalDirectory,
                scheduler,
                Size.megabytes(100L),
                Duration.standardHours(1),
                Size.megabytes(5L),
                Duration.standardHours(1),
                1_000_000,
                Duration.standardMinutes(1),
                new MetricRegistry());
            fail("Expected exception");
        } catch (Exception e) {
            assertThat(e)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessageStartingWith("kafka.common.KafkaException: Failed to acquire lock on file .lock in")
                .hasCauseExactlyInstanceOf(KafkaException.class);
        }
    }
}
