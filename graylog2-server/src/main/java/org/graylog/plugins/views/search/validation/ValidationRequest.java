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
package org.graylog.plugins.views.search.validation;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
public abstract class ValidationRequest {

    @NotNull
    public abstract BackendQuery query();

    /**
     * For validation, we assume that the filter ist another query-string. This is a different approach
     * than the one used in the Search!
     */
    @NotNull
    public abstract Optional<BackendQuery> filter();

    public String getCombinedQueryWithFilter() {
        ElasticsearchQueryString esQuery = (ElasticsearchQueryString) query();
        return filter()
                .map(f -> (ElasticsearchQueryString) f)
                .map(esQuery::concatenate)
                .orElse(esQuery)
                .queryString();
    }

    @NotNull
    public abstract TimeRange timerange();

    @NotNull
    public abstract Set<String> streams();

    @NotNull
    public abstract ImmutableSet<Parameter> parameters();

    public static Builder builder() {
        return new AutoValue_ValidationRequest.Builder().parameters(ImmutableSet.<Parameter>builder().build());
    }

    public boolean isEmptyQuery() {
        return StringUtils.isEmpty(query().queryString());
    }

    public String rawQuery() {
        return query().queryString();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder query(@NotNull BackendQuery query);

        public abstract Builder filter(@Nullable BackendQuery filter);

        public abstract Builder streams(@NotNull Set<String> streams);

        public abstract Builder timerange(@NotNull TimeRange timerange);

        public abstract Builder parameters(@NotNull ImmutableSet<Parameter> parameters);

        public abstract ValidationRequest build();

        public static Builder builder() {
            return new AutoValue_ValidationRequest.Builder();
        }
    }
}
