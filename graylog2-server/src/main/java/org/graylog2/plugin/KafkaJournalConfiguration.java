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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Size;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import org.joda.time.Duration;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Objects;

public class KafkaJournalConfiguration {
    private static final String PREFIX = "message_journal_";

    public KafkaJournalConfiguration() { }

    @JsonCreator
    public KafkaJournalConfiguration(@NotNull @JsonProperty("directory") File messageJournalDir,
                                     @JsonProperty("segment_size") long messageJournalSegmentSize,
                                     @JsonProperty("segment_age") Duration messageJournalSegmentAge,
                                     @JsonProperty("max_size") long messageJournalMaxSize,
                                     @JsonProperty("max_age") Duration messageJournalMaxAge,
                                     @JsonProperty("flush_interval") long messageJournalFlushInterval,
                                     @JsonProperty("flush_age") Duration messageJournalFlushAge,
                                     @JsonProperty("check_enabled") boolean messageJournalCheckEnabled,
                                     @JsonProperty("check_stop_inputs") boolean messageJournalCheckStopInputs,
                                     @JsonProperty("check_interval") Duration messageJournalCheckInterval,
                                     @JsonProperty("check_disk_free_percent") int messageJournalCheckDiskFreePercent) {
        this.messageJournalDir = Objects.requireNonNull(messageJournalDir);
        this.messageJournalSegmentSize = Size.bytes(messageJournalSegmentSize);
        this.messageJournalSegmentAge = messageJournalSegmentAge;
        this.messageJournalMaxSize = Size.bytes(messageJournalMaxSize);
        this.messageJournalMaxAge = messageJournalMaxAge;
        this.messageJournalFlushInterval = messageJournalFlushInterval;
        this.messageJournalFlushAge = messageJournalFlushAge;
        this.messageJournalCheckEnabled = messageJournalCheckEnabled;
        this.messageJournalCheckStopInputs = messageJournalCheckStopInputs;
        this.messageJournalCheckInterval = messageJournalCheckInterval;
        this.messageJournalCheckDiskFreePercent = messageJournalCheckDiskFreePercent;
    }

    @Parameter(value = PREFIX + "dir", required = true)
    @JsonProperty("directory")
    private File messageJournalDir = new File("data/journal");

    @Parameter(PREFIX + "segment_size")
    @JsonProperty("segment_size")
    private Size messageJournalSegmentSize = Size.megabytes(100L);

    @Parameter(PREFIX + "segment_age")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("segment_age")
    private Duration messageJournalSegmentAge = Duration.standardHours(1L);

    @Parameter(PREFIX + "max_size")
    @JsonProperty("max_size")
    private Size messageJournalMaxSize = Size.gigabytes(5L);

    @Parameter(PREFIX + "max_age")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("max_age")
    private Duration messageJournalMaxAge = Duration.standardHours(12L);

    @Parameter(PREFIX + "flush_interval")
    @JsonProperty("flush_interval")
    private long messageJournalFlushInterval = 1_000_000L;

    @Parameter(PREFIX + "flush_age")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("flush_age")
    private Duration messageJournalFlushAge = Duration.standardMinutes(1L);

    @Parameter(PREFIX + "check_enabled")
    @JsonProperty("check_enabled")
    private boolean messageJournalCheckEnabled = true;

    @Parameter(PREFIX + "check_stop_inputs")
    @JsonProperty("check_stop_inputs")
    private boolean messageJournalCheckStopInputs = true;

    @Parameter(PREFIX + "check_interval")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("check_interval")
    private Duration messageJournalCheckInterval = Duration.standardMinutes(10L);

    @Parameter(value = PREFIX + "check_disk_free_percent", validators = PositiveIntegerValidator.class)
    @JsonProperty("check_disk_free_percent")
    private int messageJournalCheckDiskFreePercent = 5;

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

    public boolean isMessageJournalCheckEnabled() {
        return messageJournalCheckEnabled;
    }

    public boolean isMessageJournalCheckStopInputs() {
        return messageJournalCheckStopInputs;
    }

    public Duration getMessageJournalCheckInterval() {
        return messageJournalCheckInterval;
    }

    public int getMessageJournalCheckDiskFreePercent() {
        return messageJournalCheckDiskFreePercent;
    }
}
