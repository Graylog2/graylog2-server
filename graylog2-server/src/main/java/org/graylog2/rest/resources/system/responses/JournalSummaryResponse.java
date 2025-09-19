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
package org.graylog2.rest.resources.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.joschi.jadconfig.util.Size;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
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
                                                       KafkaJournalConfigurationSummary kafkaJournalConfiguration) {
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
                                                @JsonProperty("journal_config") KafkaJournalConfigurationSummary kafkaJournalConfiguration) {
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
                                                KafkaJournalConfigurationSummary kafkaJournalConfiguration) {
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
    @JsonProperty("enabled")
    public abstract boolean enabled();

    @JsonProperty("append_events_per_second")
    public abstract long appendEventsPerSecond();

    @JsonProperty("read_events_per_second")
    public abstract long readEventsPerSecond();

    @JsonProperty("uncommitted_journal_entries")
    public abstract long uncommittedJournalEntries();

    @JsonProperty("journal_size")
    public abstract Size journalSize();

    @JsonProperty("journal_size_limit")
    public abstract Size journalSizeLimit();

    @JsonProperty("number_of_segments")
    public abstract int numberOfSegments();

    @JsonProperty("oldest_segment")
    @Nullable
    public abstract DateTime oldestSegment();

    @JsonProperty("journal_config")
    @Nullable
    public abstract KafkaJournalConfigurationSummary journalConfig();
}
