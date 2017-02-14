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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class StreamsChangedEvent {
    private static final String FIELD_STREAM_IDS = "stream_ids";

    @JsonProperty(FIELD_STREAM_IDS)
    public abstract ImmutableSet<String> streamIds();

    @JsonCreator
    public static StreamsChangedEvent create(@JsonProperty(FIELD_STREAM_IDS) ImmutableSet<String> streamIds) {
        return new AutoValue_StreamsChangedEvent(streamIds);
    }

    public static StreamsChangedEvent create(String streamId) {
        return create(ImmutableSet.of(streamId));
    }
}