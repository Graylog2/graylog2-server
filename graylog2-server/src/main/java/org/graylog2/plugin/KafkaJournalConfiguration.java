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
package org.graylog2.plugin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Size;
import org.joda.time.Duration;

import java.io.File;

public class KafkaJournalConfiguration {

    @Parameter(value = "message_journal_dir", required = true)
    @JsonProperty("directory")
    private File messageJournalDir = new File("data/journal");

    @Parameter("message_journal_segment_size")
    @JsonProperty("segment_size")
    private Size messageJournalSegmentSize = Size.megabytes(100L);

    @Parameter("message_journal_segment_age")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("segment_age")
    private Duration messageJournalSegmentAge = Duration.standardHours(1L);

    @Parameter("message_journal_max_size")
    @JsonProperty("max_size")
    private Size messageJournalMaxSize = Size.gigabytes(5L);

    @Parameter("message_journal_max_age")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("max_age")
    private Duration messageJournalMaxAge = Duration.standardHours(12L);

    @Parameter("message_journal_flush_interval")
    @JsonProperty("flush_interval")
    private long messageJournalFlushInterval = 1_000_000L;

    @Parameter("message_journal_flush_age")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("flush_age")
    private Duration messageJournalFlushAge = Duration.standardMinutes(1L);

    public File getMessageJournalDir() {
        return messageJournalDir;
    }

    public Size getMessageJournalSegmentSize() {
        return messageJournalSegmentSize;
    }

    public Duration getMessageJournalSegmentAge() {
        return messageJournalSegmentAge;
    }

    public Duration getMessageJournalMaxAge() {
        return messageJournalMaxAge;
    }

    public Size getMessageJournalMaxSize() {
        return messageJournalMaxSize;
    }

    public long getMessageJournalFlushInterval() {
        return messageJournalFlushInterval;
    }

    public Duration getMessageJournalFlushAge() {
        return messageJournalFlushAge;
    }
}
