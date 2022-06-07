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
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.of;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = SearchDTO.Builder.class)
public abstract class SearchDTO {
    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract LinkedHashSet<QueryDTO> queries();

    @JsonProperty
    public abstract Set<Parameter> parameters();

    static SearchDTO fromSearch(Search search) {
        return SearchDTO.Builder.create()
                .id(search.id())
                .parameters(search.parameters())
                .queries(search.queries()
                        .stream()
                        .map(QueryDTO::fromQuery)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder id(String id);

        public abstract String id();

        @JsonProperty
        public abstract Builder queries(LinkedHashSet<QueryDTO> queries);

        @JsonProperty
        public abstract Builder parameters(Set<Parameter> parameters);

        public abstract SearchDTO build();

        @JsonCreator
        static Builder create() {
            return new AutoValue_SearchDTO.Builder()
                    .queries(new LinkedHashSet<>())
                    .parameters(of());
        }
    }

    Search toSearch() {
        final ImmutableSet<Query> queries = queries().stream()
                .map(QueryDTO::toQuery)
                .collect(ImmutableSet.toImmutableSet());
        return Search.builder()
                .id(id())
                .queries(queries)
                .parameters(ImmutableSet.copyOf(parameters()))
                .build();
    }
}
