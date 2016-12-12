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
package org.graylog2.rest.resources.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.joschi.jadconfig.util.Size;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class JournalSummaryResponse {

    public static JournalSummaryResponse createDisabled() {
        return JournalSummaryResponse.create(false, 0, 0, 0, Size.bytes(0), Size.bytes(0), 0, null, null);
    }

    public static JournalSummaryResponse createEnabled(long appendEventsPerSec,
                                                       long readEventsPerSec,
                                                       long uncommittedJournalEntries,
                                                       Size journalSize,
                                                       Size journalSizeLimit,
                                                       int numberOfSegments,
                                                       DateTime oldestSegment,
                                                       KafkaJournalConfiguration kafkaJournalConfiguration) {
        return JournalSummaryResponse.create(true,
                appendEventsPerSec,
                readEventsPerSec,
                uncommittedJournalEntries,
                journalSize,
                journalSizeLimit,
                numberOfSegments,
                oldestSegment,
                kafkaJournalConfiguration);
    }

    @JsonCreator
    public static JournalSummaryResponse create(@JsonProperty("enabled") boolean enabled,
                                                @JsonProperty("append_events_per_second") long appendEventsPerSec,
                                                @JsonProperty("read_events_per_second") long readEventsPerSec,
                                                @JsonProperty("uncommitted_journal_entries") long uncommittedJournalEntries,
                                                @JsonProperty("journal_size") long journalSize,
                                                @JsonProperty("journal_size_limit") long journalSizeLimit,
                                                @JsonProperty("number_of_segments") int numberOfSegments,
                                                @JsonProperty("oldest_segment") DateTime oldestSegment,
                                                @JsonProperty("journal_config") KafkaJournalConfiguration kafkaJournalConfiguration) {
        return JournalSummaryResponse.create(enabled,
                appendEventsPerSec,
                readEventsPerSec,
                uncommittedJournalEntries,
                Size.bytes(journalSize),
                Size.bytes(journalSizeLimit),
                numberOfSegments,
                oldestSegment,
                kafkaJournalConfiguration);
    }

    public static JournalSummaryResponse create(boolean enabled,
                                                long appendEventsPerSec,
                                                long readEventsPerSec,
                                                long uncommittedJournalEntries,
                                                Size journalSize,
                                                Size journalSizeLimit,
                                                int numberOfSegments,
                                                DateTime oldestSegment,
                                                KafkaJournalConfiguration kafkaJournalConfiguration) {
        return new AutoValue_JournalSummaryResponse(enabled,
                appendEventsPerSec,
                readEventsPerSec,
                uncommittedJournalEntries,
                journalSize,
                journalSizeLimit,
                numberOfSegments,
                oldestSegment,
                kafkaJournalConfiguration);
    }

    // keep the fields in the same order as the auto value constructor params!
    @JsonProperty
    public abstract boolean enabled();

    @JsonProperty
    public abstract long appendEventsPerSecond();

    @JsonProperty
    public abstract long readEventsPerSecond();

    @JsonProperty
    public abstract long uncommittedJournalEntries();

    @JsonProperty
    public abstract Size journalSize();

    @JsonProperty
    public abstract Size journalSizeLimit();

    @JsonProperty
    public abstract int numberOfSegments();

    @JsonProperty
    @Nullable
    public abstract DateTime oldestSegment();

    @JsonProperty
    @Nullable
    public abstract KafkaJournalConfiguration journalConfig();
}
