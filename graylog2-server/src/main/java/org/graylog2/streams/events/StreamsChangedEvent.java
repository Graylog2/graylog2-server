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
package org.graylog2.streams.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_StreamsChangedEvent.Builder.class)
@AutoValue
public abstract class StreamsChangedEvent {
    private static final String FIELD_STREAM_IDS = "stream_ids";

    @JsonProperty(FIELD_STREAM_IDS)
    public abstract ImmutableSet<String> streamIds();

    public static StreamsChangedEvent create(String streamId) {
        return builder().addStreamId(streamId).build();
    }

    public static Builder builder() {
        return new AutoValue_StreamsChangedEvent.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_STREAM_IDS)
        public Builder addStreamIds(Set<String> streamIds) {
            streamIdsBuilder().addAll(streamIds);
            return this;
        }

        public Builder addStreamId(String streamId) {
            streamIdsBuilder().add(streamId);
            return this;
        }

        abstract ImmutableSet.Builder<String> streamIdsBuilder();

        public abstract StreamsChangedEvent build();
    }
}