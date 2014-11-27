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

import com.google.common.collect.Iterators;
import org.graylog2.Graylog2BaseTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static com.google.common.base.Charsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class KafkaJournalTest extends Graylog2BaseTest {

    private ScheduledThreadPoolExecutor scheduler;

    @BeforeClass
    public void setup() {
        scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.prestartCoreThread();
    }

    @AfterClass
    public void after() {
        scheduler.shutdown();
    }

    @Test
    public void writeAndRead() throws IOException {
        final Path journalDir = Files.createTempDirectory("journal");
        final Journal journal = new KafkaJournal(journalDir.toFile().getAbsolutePath(), scheduler, 100 * 1024 * 1024);

        final byte[] idBytes = "id".getBytes(UTF_8);
        final byte[] messageBytes = "message".getBytes(UTF_8);

        final long position = journal.write(idBytes, messageBytes);
        final List<Journal.JournalReadEntry> messages = journal.read(1);

        final Journal.JournalReadEntry firstMessage = Iterators.getOnlyElement(messages.iterator());

        assertEquals(new String(firstMessage.getPayload(), UTF_8), "message");

        deleteDirectory(journalDir.toFile());
    }

    @Test
    public void readAtLeastOne() throws Exception {
        final Path journalDir = Files.createTempDirectory("journal");
        final Journal journal = new KafkaJournal(journalDir.toFile().getAbsolutePath(), scheduler, 100 * 1024 * 1024);

        final byte[] idBytes = "id".getBytes(UTF_8);
        final byte[] messageBytes = "message1".getBytes(UTF_8);

        final long position = journal.write(idBytes, messageBytes);

        // Trying to read 0 should always read at least 1 entry.
        final List<Journal.JournalReadEntry> messages = journal.read(0);

        final Journal.JournalReadEntry firstMessage = Iterators.getOnlyElement(messages.iterator());

        assertEquals(new String(firstMessage.getPayload(), UTF_8), "message1");

        deleteDirectory(journalDir.toFile());
    }

}