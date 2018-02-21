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
package org.graylog.plugins.map.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Set;

@JsonAutoDetect
@AutoValue
public abstract class MapDataSearchRequest {
    @JsonProperty("query")
    public abstract String query();

    @JsonProperty("timerange")
    public abstract TimeRange timerange();

    @JsonProperty("limit")
    public abstract int limit();

    @JsonProperty("fields")
    public abstract Set<String> fields();

    @JsonProperty("stream_id")
    @Nullable
    public abstract String streamId();

    @JsonCreator
    public static MapDataSearchRequest create(@JsonProperty("query") String query,
                                              @JsonProperty("timerange") TimeRange timerange,
                                              @JsonProperty("limit") int limit,
                                              @JsonProperty("fields") Set<String> fields,
                                              @JsonProperty("stream_id") @Nullable String streamId) {
        return builder()
                .query(query)
                .timerange(timerange)
                .limit(limit)
                .fields(fields)
                .streamId(streamId)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_MapDataSearchRequest.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder query(String query);
        public abstract Builder timerange(TimeRange timerange);
        public abstract Builder limit(int limit);
        public abstract Builder fields(Set<String> fields);
        public abstract Builder streamId(String streamId);

        public abstract MapDataSearchRequest build();
    }
}