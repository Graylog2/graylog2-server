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
package org.graylog.plugins.views.search.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Filter;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@AutoValue
@JsonTypeName(StreamFilter.NAME)
@JsonDeserialize(builder = StreamFilter.Builder.class)
public abstract class StreamFilter implements Filter {
    public static final String NAME = "stream";

    @Override
    @JsonProperty
    public abstract String type();

    @Override
    @Nullable
    @JsonProperty
    public abstract Set<Filter> filters();

    @Nullable
    @JsonProperty("id")
    public abstract String streamId();

    @Nullable
    @JsonProperty("title")
    public abstract String streamTitle();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public static StreamFilter ofId(String id) {
        return builder().streamId(id).build();
    }

    public static Filter anyIdOf(String... ids) {
        final Set<Filter> streamFilters = Arrays.stream(ids)
                .map(StreamFilter::ofId)
                .collect(toSet());
        return OrFilter.builder()
                .filters(streamFilters)
                .build();
    }

    @Override
    public Filter.Builder toGenericBuilder() {
        return toBuilder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements Filter.Builder {
        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder filters(@Nullable Set<Filter> filters);

        @JsonProperty("id")
        public abstract Builder streamId(@Nullable String streamId);

        @JsonProperty("title")
        public abstract Builder streamTitle(@Nullable String streamTitle);

        public abstract StreamFilter build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_StreamFilter.Builder().type(NAME);
        }
    }
}
