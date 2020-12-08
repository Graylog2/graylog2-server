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
package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.MutableGraph;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.aggregation.AggregationConditions;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.events.processor.aggregation.AggregationSeries;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.streams.Stream;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@AutoValue
@JsonTypeName(AggregationEventProcessorConfigEntity.TYPE_NAME)
@JsonDeserialize(builder = AggregationEventProcessorConfigEntity.Builder.class)
public abstract class AggregationEventProcessorConfigEntity implements EventProcessorConfigEntity {
    public static final String TYPE_NAME = "aggregation-v1";

    private static final String FIELD_QUERY = "query";
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_GROUP_BY = "group_by";
    private static final String FIELD_SERIES = "series";
    private static final String FIELD_CONDITIONS = "conditions";
    private static final String FIELD_SEARCH_WITHIN_MS = "search_within_ms";
    private static final String FIELD_EXECUTE_EVERY_MS = "execute_every_ms";

    @JsonProperty(FIELD_QUERY)
    public abstract ValueReference query();

    @JsonProperty(FIELD_STREAMS)
    public abstract ImmutableSet<String> streams();

    @JsonProperty(FIELD_GROUP_BY)
    public abstract List<String> groupBy();

    @JsonProperty(FIELD_SERIES)
    public abstract List<AggregationSeries> series();

    @JsonProperty(FIELD_CONDITIONS)
    public abstract Optional<AggregationConditions> conditions();

    @JsonProperty(FIELD_SEARCH_WITHIN_MS)
    public abstract long searchWithinMs();

    @JsonProperty(FIELD_EXECUTE_EVERY_MS)
    public abstract long executeEveryMs();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements EventProcessorConfigEntity.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_AggregationEventProcessorConfigEntity.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(ValueReference query);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(ImmutableSet<String> streams);

        @JsonProperty(FIELD_GROUP_BY)
        public abstract Builder groupBy(List<String> groupBy);

        @JsonProperty(FIELD_SERIES)
        public abstract Builder series(List<AggregationSeries> series);

        @JsonProperty(FIELD_CONDITIONS)
        public abstract Builder conditions(@Nullable AggregationConditions conditions);

        @JsonProperty(FIELD_SEARCH_WITHIN_MS)
        public abstract Builder searchWithinMs(long searchWithinMs);

        @JsonProperty(FIELD_EXECUTE_EVERY_MS)
        public abstract Builder executeEveryMs(long executeEveryMs);

        public abstract AggregationEventProcessorConfigEntity build();
    }

    @Override
    public EventProcessorConfig toNativeEntity(Map<String, ValueReference> parameters,
                                               Map<EntityDescriptor, Object> nativeEntities) {
        final ImmutableSet<String> streamSet = ImmutableSet.copyOf(
                streams().stream()
                        .map(id -> EntityDescriptor.create(id, ModelTypes.STREAM_V1))
                        .map(nativeEntities::get)
                        .map(object -> {
                            if (object == null) {
                                throw new ContentPackException("Missing Stream for event definition");
                            } else if (object instanceof Stream) {
                                Stream stream = (Stream) object;
                                return stream.getId();
                            } else {
                                throw new ContentPackException(
                                        "Invalid type for stream Stream for event definition: " + object.getClass());
                            }
                        }).collect(Collectors.toSet())
        );
        return AggregationEventProcessorConfig.builder()
                .type(type())
                .query(query().asString(parameters))
                .streams(streamSet)
                .groupBy(groupBy())
                .series(series())
                .conditions(conditions().orElse(null))
                .executeEveryMs(executeEveryMs())
                .searchWithinMs(searchWithinMs())
                .build();
    }

    @Override
    public void resolveForInstallation(EntityV1 entity,
                                       Map<String, ValueReference> parameters,
                                       Map<EntityDescriptor,Entity> entities,
                                       MutableGraph<Entity> graph) {
        streams().stream()
                .map(ModelId::of)
                .map(modelId -> EntityDescriptor.create(modelId, ModelTypes.STREAM_V1))
                .map(entities::get)
                .filter(Objects::nonNull)
                .forEach(stream -> graph.putEdge(entity, stream));
    }
}
