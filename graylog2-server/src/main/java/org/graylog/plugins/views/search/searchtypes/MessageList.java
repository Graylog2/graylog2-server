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
package org.graylog.plugins.views.search.searchtypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.graph.MutableGraph;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.SearchTypeBuilder;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.rest.SearchTypeExecutionState;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.MessageListEntity;
import org.graylog2.contentpacks.model.entities.SearchTypeEntity;
import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorImpl;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.models.messages.responses.DecorationStats;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@AutoValue
@JsonTypeName(MessageList.NAME)
@JsonDeserialize(builder = MessageList.Builder.class)
public abstract class MessageList implements SearchType {
    public static final String NAME = "messages";

    @Override
    @JsonProperty
    public abstract String type();

    @Override
    @Nullable
    @JsonProperty
    public abstract String id();

    @Override
    @JsonProperty
    public abstract Optional<String> name();

    @Nullable
    @Override
    public abstract Filter filter();

    @Override
    @JsonProperty(FIELD_SEARCH_FILTERS)
    public abstract List<UsedSearchFilter> filters();

    @JsonProperty
    public abstract int limit();

    @JsonProperty
    public abstract int offset();

    @Nullable
    @JsonProperty
    public abstract List<Sort> sort();

    @JsonProperty
    public abstract List<String> fields();

    @JsonProperty
    public abstract List<Decorator> decorators();

    @Override
    public boolean isExportable() {
        return true;
    }

    @JsonCreator
    public static Builder builder() {
        return new AutoValue_MessageList.Builder()
                .type(NAME)
                .limit(150)
                .offset(0)
                .filters(Collections.emptyList())
                .streams(Collections.emptySet())
                .streamCategories(Collections.emptySet())
                .decorators(Collections.emptyList())
                .fields(Collections.emptyList());
    }

    public abstract Builder toBuilder();

    @Override
    public SearchType applyExecutionContext(SearchTypeExecutionState executionState) {

        if (executionState.limit().isPresent() || executionState.offset().isPresent()) {
            final Builder builder = toBuilder();
            executionState.limit().ifPresent(builder::limit);
            executionState.offset().ifPresent(builder::offset);
            return builder.build();
        }
        return this;
    }

    @AutoValue.Builder
    public abstract static class Builder implements SearchTypeBuilder {
        @JsonCreator
        public static Builder createDefault() {
            return builder()
                    .filters(Collections.emptyList())
                    .streamCategories(Collections.emptySet())
                    .streams(Collections.emptySet());
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        abstract String id();

        @JsonProperty
        public abstract Builder name(@Nullable String name);

        @JsonProperty
        public abstract Builder filter(@Nullable Filter filter);

        @JsonProperty(FIELD_SEARCH_FILTERS)
        public abstract Builder filters(List<UsedSearchFilter> filters);

        @JsonProperty
        public abstract Builder fields(List<String> fields);

        @JsonProperty
        public Builder timerange(@Nullable TimeRange timerange) {
            return timerange(timerange == null ? null : DerivedTimeRange.of(timerange));
        }
        public abstract Builder timerange(@Nullable DerivedTimeRange timerange);

        @JsonProperty
        public abstract Builder query(@Nullable BackendQuery query);

        @JsonProperty
        public abstract Builder streams(Set<String> streams);

        @JsonProperty
        public abstract Builder streamCategories(Set<String> streamCategories);

        @JsonProperty
        public abstract Builder limit(int limit);

        @JsonProperty
        public abstract Builder offset(int offset);

        @JsonProperty
        public abstract Builder sort(@Nullable List<Sort> sort);

        @JsonProperty("decorators")
        public Builder _decorators(List<DecoratorImpl> decorators) {
            return decorators(new ArrayList<>(decorators));
        }

        public abstract Builder decorators(List<Decorator> decorators);

        abstract MessageList autoBuild();

        public MessageList build() {
            if (id() == null) {
                id(UUID.randomUUID().toString());
            }
            return autoBuild();
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonDeserialize(builder = Result.Builder.class)
    @JsonTypeName(MessageList.NAME)
    public abstract static class Result implements SearchType.Result {

        @Override
        @JsonProperty
        public abstract String id();

        @Override
        public abstract String type();

        @JsonProperty
        public abstract List<ResultMessageSummary> messages();

        @JsonProperty
        public abstract Optional<DecorationStats> decorationStats();

        @JsonProperty
        public abstract AbsoluteRange effectiveTimerange();

        @JsonProperty
        public abstract long totalResults();

        public static Builder builder() {
            return new AutoValue_MessageList_Result.Builder().type(MessageList.NAME);
        }

        public static Builder result(String searchTypeId) {
            return builder().id(searchTypeId);
        }

        @AutoValue.Builder
        public abstract static class Builder {

            @JsonCreator
            public static Result.Builder create() {
                return new AutoValue_MessageList_Result.Builder().type(MessageList.NAME);
            }

            @JsonProperty
            public abstract Builder id(String id);

            @JsonProperty
            public abstract Builder name(@Nullable String name);

            @JsonProperty
            public abstract Builder type(String type);

            @JsonProperty
            public abstract Builder messages(List<ResultMessageSummary> messages);

            @JsonProperty
            public abstract Builder totalResults(long totalResults);

            @JsonProperty
            public abstract Builder decorationStats(DecorationStats decorationStats);

            @JsonProperty
            public abstract Builder effectiveTimerange(AbsoluteRange effectiveTimerange);

            public abstract Result build();
        }
    }

    @Override
    public SearchTypeEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return MessageListEntity.builder()
                .decorators(decorators())
                .streams(mappedStreams(entityDescriptorIds))
                .streamCategories(streamCategories())
                .timerange(timerange().orElse(null))
                .limit(limit())
                .offset(offset())
                .filter(filter())
                .filters(filters().stream().map(filter -> filter.toContentPackEntity(entityDescriptorIds)).toList())
                .id(id())
                .name(name().orElse(null))
                .query(query().orElse(null))
                .type(type())
                .sort(sort())
                .build();
    }


    @Override
    public void resolveNativeEntity(EntityDescriptor entityDescriptor, MutableGraph<EntityDescriptor> mutableGraph) {
        filters().forEach(filter -> filter.resolveNativeEntity(entityDescriptor, mutableGraph));
    }
}
