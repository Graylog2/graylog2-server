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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.rest.SearchTypeExecutionState;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog.plugins.views.search.timeranges.OffsetRange;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.PivotEntity;
import org.graylog2.contentpacks.model.entities.SearchTypeEntity;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.ImmutableList.of;

@AutoValue
@JsonTypeName(Pivot.NAME)
@JsonDeserialize(builder = Pivot.Builder.class)
public abstract class Pivot implements SearchType {
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

    public abstract Builder toBuilder();

    @Override
    public SearchType applyExecutionContext(SearchTypeExecutionState state) {
        return this;
    }

    @Override
    public SearchType withQuery(BackendQuery query) {
        return toBuilder().query(query).build();
    }

    @Override
    public SearchType withFilter(Filter filter) {
        return toBuilder().filter(filter).build();
    }

    @Override
    public SearchType withFilters(List<UsedSearchFilter> filters) {
        return toBuilder().filters(filters).build();
    }

    public static Builder builder() {
        return new AutoValue_Pivot.Builder()
                .type(NAME)
                .rowGroups(of())
                .columnGroups(of())
                .sort(of())
                .filters(of())
                .streams(Collections.emptySet());
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder createDefault() {
            return builder()
                    .sort(Collections.emptyList())
                    .filters(Collections.emptyList())
                    .streams(Collections.emptySet());
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        abstract String id();

        @JsonProperty
        public abstract Builder name(@Nullable String name);

        @JsonProperty("row_groups")
        public abstract Builder rowGroups(List<BucketSpec> rowGroups);

        public Builder rowGroups(BucketSpec... rowGroups) {
            return rowGroups(List.of(rowGroups));
        }

        @JsonProperty("column_groups")
        public abstract Builder columnGroups(List<BucketSpec> columnGroups);

        public Builder columnGroups(BucketSpec... columnGroups) {
            return columnGroups(List.of(columnGroups));
        }

        @JsonProperty
        public abstract Builder series(List<SeriesSpec> series);

        public Builder series(SeriesSpec... series) {
            return series(List.of(series));
        }

        @JsonProperty
        public abstract Builder sort(List<SortSpec> sort);

        public Builder sort(SortSpec... sort) {
            return sort(List.of(sort));
        }

        @JsonProperty
        public abstract Builder rollup(boolean rollup);

        @JsonProperty
        public abstract Builder filter(@Nullable Filter filter);

        @JsonProperty(FIELD_SEARCH_FILTERS)
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

        @JsonProperty
        public abstract Builder streams(Set<String> streams);

        abstract Pivot autoBuild();

        public Pivot build() {
            if (id() == null) {
                id(UUID.randomUUID().toString());
            }
            return autoBuild();
        }
    }

    @Override
    public SearchTypeEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        PivotEntity.Builder builder = PivotEntity.builder()
                .sort(sort())
                .streams(mappedStreams(entityDescriptorIds))
                .timerange(timerange().orElse(null))
                .columnGroups(columnGroups())
                .rowGroups(rowGroups())
                .filter(filter())
                .filters(filters())
                .query(query().orElse(null))
                .id(id())
                .name(name().orElse(null))
                .rollup(rollup())
                .series(series())
                .type(type());
        return builder.build();
    }
}
