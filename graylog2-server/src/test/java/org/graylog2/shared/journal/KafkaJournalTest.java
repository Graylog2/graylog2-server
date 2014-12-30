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

import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import kafka.log.LogSegment;
import org.graylog2.Graylog2BaseTest;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static com.google.common.base.Charsets.UTF_8;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.fileFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class KafkaJournalTest extends Graylog2BaseTest {
    private static final int BULK_SIZE = 200;

    private ScheduledThreadPoolExecutor scheduler;
    private File journalDirectory;

    @BeforeClass
    public void before() {
        scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.prestartCoreThread();
    }

    @AfterClass
    public void after() {
        scheduler.shutdown();
    }

    @BeforeMethod
    public void setUp() throws IOException {
        journalDirectory = Files.createTempDirectory("journal").toFile();
    }

    @AfterMethod
    public void tearDown() {
        deleteDirectory(journalDirectory);
    }

    @Test
    public void writeAndRead() throws IOException {
        final Journal journal = new KafkaJournal(journalDirectory, scheduler, Size.megabytes(100l),
                Size.megabytes(5l), Duration.standardHours(1), new EventBus(), new MetricRegistry(),
                mock(ProcessBuffer.class));

        final byte[] idBytes = "id".getBytes(UTF_8);
        final byte[] messageBytes = "message".getBytes(UTF_8);

        final long position = journal.write(idBytes, messageBytes);
        final List<Journal.JournalReadEntry> messages = journal.read(1);

        final Journal.JournalReadEntry firstMessage = Iterators.getOnlyElement(messages.iterator());

        assertEquals(new String(firstMessage.getPayload(), UTF_8), "message");
    }

    @Test
    public void readAtLeastOne() throws Exception {
        final Journal journal = new KafkaJournal(journalDirectory, scheduler, Size.megabytes(100l),
                Size.megabytes(5l), Duration.standardHours(1), new EventBus(), new MetricRegistry(),
                mock(ProcessBuffer.class));

        final byte[] idBytes = "id".getBytes(UTF_8);
        final byte[] messageBytes = "message1".getBytes(UTF_8);

        final long position = journal.write(idBytes, messageBytes);

        // Trying to read 0 should always read at least 1 entry.
        final List<Journal.JournalReadEntry> messages = journal.read(0);

        final Journal.JournalReadEntry firstMessage = Iterators.getOnlyElement(messages.iterator());

        assertEquals(new String(firstMessage.getPayload(), UTF_8), "message1");
    }

    private void createBulkChunks(KafkaJournal journal, int bulkCount) {
        // perform multiple writes to make multiple segments
        for (int currentBulk = 0; currentBulk < bulkCount; currentBulk++) {
            final List<Journal.Entry> entries = Lists.newArrayList();
            // write enough bytes in one go to be over the 1024 byte segment limit, which causes a segment roll
            for (int i = 0; i < BULK_SIZE; i++) {
                final byte[] idBytes = ("id" + i).getBytes(UTF_8);
                final byte[] messageBytes = ("message " + i).getBytes(UTF_8);

                entries.add(journal.createEntry(idBytes, messageBytes));

            }
            journal.write(entries);
        }
    }

    private int countSegmentsInDir(File messageJournalFile) {
        // let it throw
        return messageJournalFile.list(and(fileFileFilter(), suffixFileFilter(".log"))).length;
    }

    @Test
    public void segmentRotation() throws Exception {
        final KafkaJournal journal = new KafkaJournal(journalDirectory,
                scheduler,
                Size.kilobytes(1l),
                Size.kilobytes(10l),
                Duration.standardDays(1),
                new EventBus(),
                new MetricRegistry(),
                mock(ProcessBuffer.class));

        createBulkChunks(journal, 3);

        final File[] files = journalDirectory.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0, "there should be files in the journal directory");

        final File[] messageJournalDir = journalDirectory.listFiles((FileFilter) and(directoryFileFilter(),
                nameFileFilter("messagejournal-0")));
        assertTrue(messageJournalDir.length == 1);
        final File[] logFiles = messageJournalDir[0].listFiles((FileFilter) and(fileFileFilter(),
                suffixFileFilter(".log")));
        assertEquals(logFiles.length, 3, "should have two journal segments");
    }

    @Test
    public void segmentSizeCleanup() throws Exception {
        final KafkaJournal journal = new KafkaJournal(journalDirectory,
                scheduler,
                Size.kilobytes(1l),
                Size.kilobytes(10l),
                Duration.standardDays(1),
                new EventBus(),
                new MetricRegistry(),
                mock(ProcessBuffer.class));
        final File messageJournalDir = new File(journalDirectory, "messagejournal-0");
        assertTrue(messageJournalDir.exists());

        // create enough chunks so that we exceed the maximum journal size we configured
        createBulkChunks(journal, 3);

        // make sure everything is on disk
        journal.flushDirtyLogs();

        assertEquals(countSegmentsInDir(messageJournalDir), 3);

        final int cleanedLogs = journal.cleanupLogs();
        assertEquals(cleanedLogs, 1);

        final int numberOfSegments = countSegmentsInDir(messageJournalDir);
        assertEquals(numberOfSegments, 2);
    }

    @Test
    public void segmentAgeCleanup() throws Exception {
        final InstantMillisProvider clock = new InstantMillisProvider(DateTime.now());

        DateTimeUtils.setCurrentMillisProvider(clock);
        try {

            final KafkaJournal journal = new KafkaJournal(journalDirectory,
                    scheduler,
                    Size.kilobytes(1l),
                    Size.kilobytes(10l),
                    Duration.standardMinutes(1),
                    new EventBus(),
                    new MetricRegistry(),
                    mock(ProcessBuffer.class));
            final File messageJournalDir = new File(journalDirectory, "messagejournal-0");
            assertTrue(messageJournalDir.exists());

            // we need to fix up the last modified times of the actual files.
            long lastModifiedTs[] = new long[2];

            // create two chunks, 30 seconds apart
            createBulkChunks(journal, 1);
            journal.flushDirtyLogs();
            lastModifiedTs[0] = clock.getMillis();

            clock.tick(Period.seconds(30));

            createBulkChunks(journal, 1);
            journal.flushDirtyLogs();
            lastModifiedTs[1] = clock.getMillis();

            int i = 0;
            for (final LogSegment segment : journal.getSegments()) {
                assertTrue(i < 2);
                segment.lastModified_$eq(lastModifiedTs[i]);
                i++;
            }

            int cleanedLogs = journal.cleanupLogs();
            assertEquals(cleanedLogs, 0, "no segments should've been cleaned");
            assertEquals(countSegmentsInDir(messageJournalDir), 2, "two segments segment should remain");

            // move clock beyond the retention period and clean again
            clock.tick(Period.seconds(120));

            cleanedLogs = journal.cleanupLogs();
            assertEquals(cleanedLogs, 2, "two segments should've been cleaned (only one will actually be removed...)");
            assertEquals(countSegmentsInDir(messageJournalDir), 1, "one segment should remain");

        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    @Test
    public void segmentCommittedCleanup() throws Exception {
        final KafkaJournal journal = new KafkaJournal(journalDirectory,
                scheduler,
                Size.kilobytes(1l),
                Size.petabytes(1l), // never clean by size in this test
                Duration.standardDays(1),
                new EventBus(),
                new MetricRegistry(),
                mock(ProcessBuffer.class));
        final File messageJournalDir = new File(journalDirectory, "messagejournal-0");
        assertTrue(messageJournalDir.exists());

        createBulkChunks(journal, 3);

        // make sure everything is on disk
        journal.flushDirtyLogs();

        assertEquals(countSegmentsInDir(messageJournalDir), 3);

        // we haven't committed any offsets, this should not touch anything.
        final int cleanedLogs = journal.cleanupLogs();
        assertEquals(cleanedLogs, 0);

        final int numberOfSegments = countSegmentsInDir(messageJournalDir);
        assertEquals(numberOfSegments, 3);

        // mark first half of first segment committed, should not clean anything
        journal.markJournalOffsetCommitted(BULK_SIZE / 2);
        assertEquals(journal.cleanupLogs(), 0, "should not touch segments");
        assertEquals(countSegmentsInDir(messageJournalDir), 3);

        journal.markJournalOffsetCommitted(BULK_SIZE + 1);
        assertEquals(journal.cleanupLogs(), 1, "first segment should've been purged");
        assertEquals(countSegmentsInDir(messageJournalDir), 2);

        journal.markJournalOffsetCommitted(BULK_SIZE * 4);
        assertEquals(journal.cleanupLogs(), 1, "only purge one segment, not the active one");
        assertEquals(countSegmentsInDir(messageJournalDir), 1);
    }
}