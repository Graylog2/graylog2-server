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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.graph.MutableGraph;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsesSearchFilters;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.WidgetEntity;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.contentpacks.facades.StreamReferenceFacade.getStreamEntityId;

@AutoValue
@JsonDeserialize(builder = WidgetDTO.Builder.class)
@WithBeanGetter
public abstract class WidgetDTO implements ContentPackable<WidgetEntity>, UsesSearchFilters {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_FILTER = "filter";
    public static final String FIELD_SEARCH_FILTERS = "filters";
    public static final String FIELD_CONFIG = "config";
    public static final String FIELD_TIMERANGE = "timerange";
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_STREAMS = "streams";
    public static final String FIELD_STREAM_CATEGORIES = "stream_categories";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_FILTER)
    @Nullable
    public abstract String filter();

    @JsonProperty(FIELD_SEARCH_FILTERS)
    @Override
    public abstract List<UsedSearchFilter> filters();

    @JsonProperty(FIELD_TIMERANGE)
    public abstract Optional<TimeRange> timerange();

    @JsonProperty(FIELD_QUERY)
    public abstract Optional<BackendQuery> query();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_STREAM_CATEGORIES)
    @Nullable
    public abstract Set<String> streamCategories();

    @JsonProperty(FIELD_CONFIG)
    public abstract WidgetConfigDTO config();

    public static Builder builder() {
        return Builder.builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        @JsonProperty(FIELD_FILTER)
        public abstract Builder filter(@Nullable String filter);

        @JsonProperty(FIELD_SEARCH_FILTERS)
        public abstract Builder filters(List<UsedSearchFilter> filters);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(@Nullable BackendQuery query);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_STREAM_CATEGORIES)
        public abstract Builder streamCategories(@Nullable Set<String> streamCategories);

        @JsonProperty(FIELD_CONFIG)
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = WidgetDTO.FIELD_TYPE,
                visible = true,
                defaultImpl = UnknownWidgetConfigDTO.class)
        public abstract Builder config(WidgetConfigDTO config);

        public abstract WidgetDTO build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_WidgetDTO.Builder()
                    .streams(Collections.emptySet())
                    .filters(Collections.emptyList())
                    .streamCategories(Collections.emptySet());
        }
    }

    @Override
    public WidgetEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        Set<String> mappedStreams = streams().stream()
                .map(streamId -> getStreamEntityId(streamId, entityDescriptorIds))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        final WidgetEntity.Builder builder = WidgetEntity.builder()
                .id(this.id())
                .config(this.config())
                .filter(this.filter())
                .filters(filters().stream().map(filter -> filter.toContentPackEntity(entityDescriptorIds)).toList())
                .streams(mappedStreams)
                .streamCategories(streamCategories())
                .type(this.type());
        if (this.query().isPresent()) {
            builder.query(this.query().get());
        }
        if (this.timerange().isPresent()) {
            builder.timerange(this.timerange().get());
        }
        return builder.build();
    }

    @Override
    public void resolveNativeEntity(EntityDescriptor entityDescriptor, MutableGraph<EntityDescriptor> mutableGraph) {
        streams().forEach(streamId -> {
            final EntityDescriptor depStream = EntityDescriptor.builder()
                    .id(ModelId.of(streamId))
                    .type(ModelTypes.STREAM_REF_V1)
                    .build();
            mutableGraph.putEdge(entityDescriptor, depStream);
        });
        filters().forEach(filter -> filter.resolveNativeEntity(entityDescriptor, mutableGraph));
    }
}
