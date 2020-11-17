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
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.WidgetEntity;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = WidgetDTO.Builder.class)
@WithBeanGetter
public abstract class WidgetDTO implements ContentPackable<WidgetEntity> {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_FILTER = "filter";
    public static final String FIELD_CONFIG = "config";
    public static final String FIELD_TIMERANGE = "timerange";
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_STREAMS = "streams";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_FILTER)
    @Nullable
    public abstract String filter();

    @JsonProperty(FIELD_TIMERANGE)
    public abstract Optional<TimeRange> timerange();

    @JsonProperty(FIELD_QUERY)
    public abstract Optional<BackendQuery> query();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_CONFIG)
    public abstract WidgetConfigDTO config();

    public static Builder builder() {
        return Builder.builder();
    };

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        @JsonProperty(FIELD_FILTER)
        public abstract Builder filter(@Nullable String filter);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(@Nullable BackendQuery query);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

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
            return new AutoValue_WidgetDTO.Builder().streams(Collections.emptySet());
        }
    }

    @Override
    public WidgetEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        Set<String> mappedStreams = streams().stream().map(streamId ->
                entityDescriptorIds.get(EntityDescriptor.create(streamId, ModelTypes.STREAM_V1)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        final WidgetEntity.Builder builder = WidgetEntity.builder()
                .id(this.id())
                .config(this.config())
                .filter(this.filter())
                .streams(mappedStreams)
                .type(this.type());
        if (this.query().isPresent()) {
            builder.query(this.query().get());
        }
        if (this.timerange().isPresent()) {
            builder.timerange(this.timerange().get());
        }
        return builder.build();
    }
}
