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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.joda.time.Duration;

import java.nio.file.Path;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class KafkaJournalConfigurationSummary {
    private static final String FIELD_DIRECTORY = "directory";
    private static final String FIELD_SEGMENT_SIZE = "segment_size";
    private static final String FIELD_SEGMENT_AGE = "segment_age";
    private static final String FIELD_MAX_SIZE = "max_size";
    private static final String FIELD_MAX_AGE = "max_age";
    private static final String FIELD_FLUSH_INTERVAL = "flush_interval";
    private static final String FIELD_FLUSH_AGE = "flush_age";

    @JsonProperty(FIELD_DIRECTORY)
    public abstract Path directory();

    @JsonProperty(FIELD_SEGMENT_SIZE)
    public abstract long segmentSize();

    @JsonProperty(FIELD_SEGMENT_AGE)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public abstract Duration segmentAge();

    @JsonProperty(FIELD_MAX_SIZE)
    public abstract long maxSize();

    @JsonProperty(FIELD_MAX_AGE)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public abstract Duration maxAge();

    @JsonProperty(FIELD_FLUSH_INTERVAL)
    public abstract long flushInterval();

    @JsonProperty(FIELD_FLUSH_AGE)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public abstract Duration flushAge();

    public static KafkaJournalConfigurationSummary of(KafkaJournalConfiguration config) {
        return create(config.getMessageJournalDir(),
                config.getMessageJournalSegmentSize().toBytes(),
                config.getMessageJournalSegmentAge(),
                config.getMessageJournalMaxSize().toBytes(),
                config.getMessageJournalMaxAge(),
                config.getMessageJournalFlushInterval(),
                config.getMessageJournalFlushAge());
    }

    @JsonCreator
    public static KafkaJournalConfigurationSummary create(@JsonProperty(FIELD_DIRECTORY) Path directory,
                                                          @JsonProperty(FIELD_SEGMENT_SIZE) long segmentSize,
                                                          @JsonProperty(FIELD_SEGMENT_AGE) Duration segmentAge,
                                                          @JsonProperty(FIELD_MAX_SIZE) long maxSize,
                                                          @JsonProperty(FIELD_MAX_AGE) Duration maxAge,
                                                          @JsonProperty(FIELD_FLUSH_INTERVAL) long flushInterval,
                                                          @JsonProperty(FIELD_FLUSH_AGE) Duration flushAge) {
        return new AutoValue_KafkaJournalConfigurationSummary(directory, segmentSize, segmentAge, maxSize, maxAge, flushInterval, flushAge);
    }
}
