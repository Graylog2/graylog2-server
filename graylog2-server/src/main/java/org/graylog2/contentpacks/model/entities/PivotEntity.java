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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog.plugins.views.search.timeranges.OffsetRange;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableList.of;

@AutoValue
@JsonTypeName(PivotEntity.NAME)
@JsonDeserialize(builder = PivotEntity.Builder.class)
public abstract class PivotEntity implements SearchTypeEntity {
    public static final String NAME = "pivot";

    @Override
    public abstract String type();

    @Override
    @Nullable
    @JsonProperty
    public abstract String id();

    @Override
    @JsonProperty
    public abstract Optional<String> name();

    @JsonProperty("row_groups")
    public abstract List<BucketSpec> rowGroups();

    @JsonProperty("column_groups")
    public abstract List<BucketSpec> columnGroups();

    @JsonProperty
    public abstract List<SeriesSpec> series();

    @JsonProperty
    public abstract List<SortSpec> sort();

    @JsonProperty
    public abstract boolean rollup();

    @Nullable
    @Override
    public abstract Filter filter();

    @Override
    @JsonProperty(FIELD_SEARCH_FILTERS)
    public abstract List<UsedSearchFilter> filters();

    @JsonProperty("row_limit")
    public abstract Optional<Integer> rowLimit();

    @JsonProperty("column_limit")
    public abstract Optional<Integer> columnLimit();

    public abstract Builder toBuilder();

    @Override
    public Builder toGenericBuilder() {
        return toBuilder();
    }

    public static Builder builder() {
        return new AutoValue_PivotEntity.Builder()
                .type(NAME)
                .rowGroups(of())
                .columnGroups(of())
                .filters(Collections.emptyList())
                .sort(of())
                .streams(Collections.emptySet());
    }

    @AutoValue.Builder
    public static abstract class Builder implements SearchTypeEntity.Builder {
        @JsonCreator
        public static Builder createDefault() {
            return builder()
                    .filters(Collections.emptyList())
                    .sort(Collections.emptyList())
                    .streams(Collections.emptySet());
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder name(@Nullable String name);

        @JsonProperty("row_groups")
        public abstract Builder rowGroups(List<BucketSpec> rowGroups);

        @JsonProperty("column_groups")
        public abstract Builder columnGroups(List<BucketSpec> columnGroups);

        @JsonProperty("row_limit")
        public abstract Builder rowLimit(@Nullable Integer rowLimit);

        @JsonProperty("column_limit")
        public abstract Builder columnLimit(@Nullable Integer columnLimit);

        @JsonProperty
        public abstract Builder series(List<SeriesSpec> series);

        @JsonProperty
        public abstract Builder sort(List<SortSpec> sort);

        @JsonProperty
        public abstract Builder rollup(boolean rollup);

        @JsonProperty
        public abstract Builder filter(@Nullable Filter filter);

        @JsonProperty
        public abstract Builder filters(List<UsedSearchFilter> filters);

        @JsonProperty
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = false)
        @JsonSubTypes({
                @JsonSubTypes.Type(name = AbsoluteRange.ABSOLUTE, value = AbsoluteRange.class),
                @JsonSubTypes.Type(name = RelativeRange.RELATIVE, value = RelativeRange.class),
                @JsonSubTypes.Type(name = KeywordRange.KEYWORD, value = KeywordRange.class),
                @JsonSubTypes.Type(name = OffsetRange.OFFSET, value = OffsetRange.class)
        })
        public Builder timerange(@Nullable TimeRange timerange) {
            return timerange(timerange == null ? null : DerivedTimeRange.of(timerange));
        }
        public abstract Builder timerange(@Nullable DerivedTimeRange timerange);

        @JsonProperty
        public abstract Builder query(@Nullable BackendQuery query);

        @Override
        @JsonProperty
        public abstract Builder streams(Set<String> streams);

        @Override
        public abstract PivotEntity build();
    }

    @Override
    public SearchType toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        var rowGroups = rowLimit().map(rowLimit -> applyGroupLimit(rowGroups(), rowLimit)).orElse(rowGroups());
        var columnGroups = columnLimit().map(columnLimit -> applyGroupLimit(columnGroups(), columnLimit)).orElse(columnGroups());
        return Pivot.builder()
                .streams(mappedStreams(nativeEntities))
                .name(name().orElse(null))
                .sort(sort())
                .timerange(timerange().orElse(null))
                .rowGroups(rowGroups)
                .columnGroups(columnGroups)
                .series(series())
                .rollup(rollup())
                .query(query().orElse(null))
                .filter(filter())
                .filters(filters())
                .type(type())
                .id(id())
                .build();
    }

    private List<BucketSpec> applyGroupLimit(List<BucketSpec> bucketSpecs, int limit) {
        return bucketSpecs.stream()
                .map(rowGroup -> {
                    if (rowGroup instanceof Values values) {
                        return values.withLimit(limit);
                    }
                    return rowGroup;
                })
                .toList();
    }
}
