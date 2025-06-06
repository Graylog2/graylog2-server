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
package org.graylog.plugins.views.search.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.permissions.StreamPermissions;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@AutoValue
@JsonTypeName(StreamCategoryFilter.NAME)
@JsonDeserialize(builder = StreamCategoryFilter.Builder.class)
public abstract class StreamCategoryFilter implements Filter {
    public static final String NAME = "stream_category";

    @Override
    @JsonProperty
    public abstract String type();

    @Override
    @Nullable
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract Set<Filter> filters();

    @JsonProperty("category")
    public abstract String category();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public static StreamCategoryFilter ofCategory(String category) {
        return builder().category(category).build();
    }

    public Filter toStreamFilter(Function<Collection<String>, Stream<String>> categoryMappingFunction,
                                 StreamPermissions streamPermissions) {
        String[] mappedStreamIds = categoryMappingFunction.apply(List.of(category()))
                .filter(streamPermissions::canReadStream)
                .toArray(String[]::new);
        // If the streamPermissions do not allow for any of the streams to be read, nullify this filter.
        if (mappedStreamIds.length == 0) {
            return null;
        }
        // Replace this category with an OrFilter of stream IDs and then add filters if they exist.
        Filter streamFilter = StreamFilter.anyIdOf(mappedStreamIds).toGenericBuilder().build();
        if (filters() != null) {
            streamFilter = streamFilter.toGenericBuilder().filters(filters()).build();
        }
        return streamFilter;
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

        @JsonProperty("category")
        public abstract Builder category(String category);

        public abstract StreamCategoryFilter build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_StreamCategoryFilter.Builder().type(NAME);
        }
    }
}
