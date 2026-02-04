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
package org.graylog2.plugin;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.util.Size;
import org.graylog2.bindings.NamedBindingOverride;
import org.graylog2.configuration.PathConfiguration;
import org.joda.time.Duration;

import java.nio.file.Path;
import java.util.Optional;

@DocumentationSection(heading = "Kafka Settings", description = "")
public class KafkaJournalConfiguration extends PathConfiguration {

    public static final String MESSAGE_JOURNAL_DIR = "message_journal_dir";

    public KafkaJournalConfiguration() {
    }

    @Documentation("""
            The directory which will be used to store the message journal. The directory must be exclusively used by Graylog and
            must not contain any other files than the ones created by Graylog itself.

            ATTENTION:
              If you create a separate partition for the journal files and use a file system creating directories like 'lost+found'
              in the root directory, you need to create a sub directory for your journal.
              Otherwise Graylog will log an error message that the journal is corrupt and Graylog will not start.
            Default: <data_dir>/journal
            """)
    @Parameter(value = MESSAGE_JOURNAL_DIR)
    private Path messageJournalDir;

    @Documentation("tbd")
    @Parameter("message_journal_segment_size")
    private Size messageJournalSegmentSize = Size.megabytes(100L);

    @Documentation("tbd")
    @Parameter("message_journal_segment_age")
    private Duration messageJournalSegmentAge = Duration.standardHours(1L);

    @Documentation("tbd")
    @Parameter("message_journal_max_size")
    private Size messageJournalMaxSize = Size.gigabytes(5L);

    @Documentation("""
            Journal hold messages before they could be written to Elasticsearch.
            For a maximum of 12 hours or 5 GB whichever happens first.
            During normal operation the journal will be smaller.
            """)
    @Parameter("message_journal_max_age")
    private Duration messageJournalMaxAge = Duration.standardHours(12L);

    @Documentation("tbd")
    @Parameter("message_journal_flush_interval")
    private long messageJournalFlushInterval = 1_000_000L;

    @Documentation("tbd")
    @Parameter("message_journal_flush_age")
    private Duration messageJournalFlushAge = Duration.standardMinutes(1L);

    @NamedBindingOverride(value = MESSAGE_JOURNAL_DIR)
    public Path getMessageJournalDir() {
        return Optional.ofNullable(messageJournalDir).orElse(getDataDir().resolve("journal"));
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
