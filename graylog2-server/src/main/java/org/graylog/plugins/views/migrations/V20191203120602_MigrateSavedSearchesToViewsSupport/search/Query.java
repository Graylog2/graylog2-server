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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.ElasticsearchQueryString;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class Query {
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract TimeRange timerange();

    @JsonProperty
    public abstract Optional<StreamFilter> filter();

    @Nonnull
    @JsonProperty
    public abstract ElasticsearchQueryString query();

    @Nonnull
    @JsonProperty("search_types")
    public abstract Set<SearchType> searchTypes();

    public static Builder builder() {
        return new AutoValue_Query.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder id(String id);

        public abstract Builder timerange(TimeRange timeRange);

        abstract Builder query(ElasticsearchQueryString query);
        public Builder query(String query) {
            return this.query(ElasticsearchQueryString.create(query));
        }

        public abstract Builder searchTypes(Set<SearchType> searchTypes);

        public abstract Builder filter(StreamFilter filter);

        public Builder streamId(String streamId) {
            return filter(StreamFilter.create(streamId));
        }

        public abstract Query build();
    }
}
