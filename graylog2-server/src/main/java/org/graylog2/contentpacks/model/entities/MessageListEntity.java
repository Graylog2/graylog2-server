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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.graph.MutableGraph;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorImpl;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonTypeName(MessageListEntity.NAME)
@JsonDeserialize(builder = MessageListEntity.Builder.class)
public abstract class MessageListEntity implements SearchTypeEntity {
    public static final String NAME = "messages";

    @Override
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
    public abstract List<Sort> sort();

    @JsonProperty
    public abstract List<Decorator> decorators();

    @JsonCreator
    public static Builder builder() {
        return new AutoValue_MessageListEntity.Builder()
                .type(NAME)
                .limit(150)
                .offset(0)
                .streams(Collections.emptySet())
                .streamCategories(Collections.emptySet())
                .filters(Collections.emptyList())
                .decorators(Collections.emptyList());
    }

    public abstract Builder toBuilder();

    @Override
    public Builder toGenericBuilder() {
        return toBuilder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements SearchTypeEntity.Builder {
        @JsonCreator
        public static Builder createDefault() {
            return builder()
                    .filters(Collections.emptyList())
                    .streams(Collections.emptySet());
        }

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder name(@Nullable String name);

        @JsonProperty
        public abstract Builder filter(@Nullable Filter filter);

        @JsonProperty
        public abstract Builder filters(List<UsedSearchFilter> filters);

        @JsonProperty
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

        @Override
        public abstract MessageListEntity build();
    }

    @Override
    public SearchType toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        return MessageList.builder()
                .limit(limit())
                .streams(mappedStreams(nativeEntities))
                .streamCategories(streamCategories())
                .id(id())
                .offset(offset())
                .decorators(decorators())
                .timerange(timerange().orElse(null))
                .filter(filter())
                .filters(filters().stream().map(filter -> filter.toNativeEntity(parameters, nativeEntities)).toList())
                .name(name().orElse(null))
                .type(type())
                .query(query().orElse(null))
                .sort(sort())
                .build();
    }

    @Override
    public void resolveForInstallation(EntityV1 entity,
                                       Map<String, ValueReference> parameters,
                                       Map<EntityDescriptor, Entity> entities,
                                       MutableGraph<Entity> graph) {
        filters().forEach(filter -> filter.resolveForInstallation(entity, parameters, entities, graph));
    }
}
