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
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class KafkaJournalTest extends Graylog2BaseTest {

    @Test
    public void writeAndRead() throws IOException {
        final Path journalDir = Files.createTempDirectory("journal");
        final KafkaJournal journal = new KafkaJournal(journalDir.toFile().getAbsolutePath());

        final byte[] idBytes = "id".getBytes(UTF_8);
        final byte[] messageBytes = "message".getBytes(UTF_8);

        final long position = journal.write(idBytes, messageBytes);
        final List<KafkaJournal.JournalReadEntry> messages = journal.read();

        final KafkaJournal.JournalReadEntry firstMessage = Iterators.getOnlyElement(messages.iterator());

        assertEquals(new String(firstMessage.getPayload(), UTF_8), "message");

        deleteDirectory(journalDir.toFile());
    }

}