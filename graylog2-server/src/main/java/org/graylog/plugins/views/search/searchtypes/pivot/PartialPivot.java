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
package org.graylog.plugins.views.search.searchtypes.pivot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog.plugins.views.search.timeranges.OffsetRange;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableList.of;

@AutoValue
@JsonTypeName(PartialPivot.NAME)
@JsonDeserialize(builder = PartialPivot.Builder.class)
public abstract class PartialPivot {
    public static final String NAME = "pivot";

    @JsonProperty
    public abstract Optional<String> type();

    @JsonProperty
    public abstract Optional<String> id();

    @JsonProperty
    public abstract Optional<String> name();

    @JsonProperty("row_groups")
    public abstract Optional<List<BucketSpec>> rowGroups();

    @JsonProperty("column_groups")
    public abstract Optional<List<BucketSpec>> columnGroups();

    @JsonProperty
    public abstract Optional<List<SeriesSpec>> series();

    @JsonProperty
    public abstract Optional<List<SortSpec>> sort();

    @JsonProperty
    public abstract Optional<Boolean> rollup();

    @JsonProperty
    public abstract Optional<Filter> filter();

    @JsonProperty
    public abstract Optional<DerivedTimeRange> timerange();

    @JsonProperty
    public abstract Optional<BackendQuery> query();

    @JsonProperty
    public abstract Optional<Set<String>> streams();

    public abstract PartialPivot.Builder toBuilder();

    public static PartialPivot.Builder builder() {
        return new AutoValue_PartialPivot.Builder()
                .type(NAME);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static PartialPivot.Builder createDefault() {
            return builder()
                    .sort(Collections.emptyList())
                    .streams(Collections.emptySet());
        }

        @JsonProperty
        public abstract PartialPivot.Builder type(@Nullable String type);

        @JsonProperty
        public abstract PartialPivot.Builder id(@Nullable String id);

        @JsonProperty
        public abstract PartialPivot.Builder name(@Nullable String name);

        @JsonProperty("row_groups")
        public abstract PartialPivot.Builder rowGroups(@Nullable List<BucketSpec> rowGroups);

        @JsonProperty("column_groups")
        public abstract PartialPivot.Builder columnGroups(@Nullable List<BucketSpec> columnGroups);

        @JsonProperty
        public abstract PartialPivot.Builder series(@Nullable List<SeriesSpec> series);

        @JsonProperty
        public abstract PartialPivot.Builder sort(@Nullable List<SortSpec> sort);

        @JsonProperty
        public abstract PartialPivot.Builder rollup(@Nullable Boolean rollup);

        @JsonProperty
        public abstract PartialPivot.Builder filter(@Nullable Filter filter);

        @JsonProperty
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
        @JsonSubTypes({
                @JsonSubTypes.Type(name = AbsoluteRange.ABSOLUTE, value = AbsoluteRange.class),
                @JsonSubTypes.Type(name = RelativeRange.RELATIVE, value = RelativeRange.class),
                @JsonSubTypes.Type(name = KeywordRange.KEYWORD, value = KeywordRange.class),
                @JsonSubTypes.Type(name = OffsetRange.OFFSET, value = OffsetRange.class)
        })
        public PartialPivot.Builder timerange(@Nullable TimeRange timerange) {
            return timerange(timerange == null ? null : DerivedTimeRange.of(timerange));
        }
        public abstract PartialPivot.Builder timerange(@Nullable DerivedTimeRange timerange);

        @JsonProperty
        public abstract PartialPivot.Builder query(@Nullable BackendQuery query);

        @JsonProperty
        public abstract PartialPivot.Builder streams(@Nullable Set<String> streams);

        public abstract PartialPivot build();
    }
}
