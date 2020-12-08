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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableList.of;

@AutoValue
abstract class Pivot implements SearchType {
    static final String NAME = "pivot";

    @JsonProperty
    String type() {
        return NAME;
    }

    @JsonProperty
    public abstract Optional<String> name();

    @JsonProperty("row_groups")
    abstract List<BucketSpec> rowGroups();

    @JsonProperty("column_groups")
    abstract List<BucketSpec> columnGroups();

    @JsonProperty
    abstract List<SeriesSpec> series();

    @JsonProperty
    abstract List<SortSpec> sort();

    @JsonProperty
    abstract boolean rollup();

    @Nullable
    @JsonProperty
    Object filter() { return null; }

    static Builder builder() {
        return new AutoValue_Pivot.Builder()
                .rowGroups(of())
                .columnGroups(of())
                .sort(of())
                .streams(Collections.emptySet());
    }

    @AutoValue.Builder
    static abstract class Builder {

        abstract Builder id(String id);

        abstract Builder name(@Nullable String name);

        abstract Builder rowGroups(List<BucketSpec> rowGroups);

        abstract Builder columnGroups(List<BucketSpec> columnGroups);

        abstract Builder series(List<SeriesSpec> series);

        abstract Builder sort(List<SortSpec> sort);

        abstract Builder rollup(boolean rollup);

        abstract Builder timerange(@Nullable TimeRange timerange);

        abstract Builder query(@Nullable ElasticsearchQueryString query);
        Builder query(String query) {
            return query(ElasticsearchQueryString.create(query));
        }

        abstract Builder streams(Set<String> streams);

        abstract Pivot build();
    }
}
