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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;
import org.graylog.plugins.views.search.Filter;

import java.util.Set;

@AutoValue
@JsonTypeName(OrFilter.NAME)
@JsonDeserialize(builder = OrFilter.Builder.class)
public abstract class OrFilter implements Filter {
    public static final String NAME = "or";

    @Override
    @JsonProperty
    public abstract String type();

    @Override
    @JsonProperty
    public abstract Set<Filter> filters();

    public static Builder builder() {
        return Builder.create();
    }

    public static OrFilter or(Filter... filters) {
        return builder()
                .filters(Sets.newHashSet(filters))
                .build();
    }

    public abstract Builder toBuilder();

    @Override
    public Filter.Builder toGenericBuilder() {
        return toBuilder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements Filter.Builder {
        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder filters(Set<Filter> filters);

        @JsonProperty
        public abstract OrFilter build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_OrFilter.Builder().type(NAME);
        }
    }
}
