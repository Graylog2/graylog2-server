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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.QueryParam;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;

@AutoValue
public abstract class QueryMetadata {

    @JsonProperty("used_parameters_names")
    public ImmutableSet<String> usedParameterNames() {
        return usedParameters().stream().map(QueryParam::name).collect(ImmutableSet.toImmutableSet());
    }

    @JsonIgnore
    public abstract ImmutableSet<QueryParam> usedParameters();

    public static QueryMetadata empty() {
        return QueryMetadata.builder().build();
    }

    public static Builder builder() {
        return new AutoValue_QueryMetadata.Builder()
                .usedParameters(of());
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder usedParameters(Set<QueryParam> usedParameters);

        public abstract QueryMetadata build();
    }
}
