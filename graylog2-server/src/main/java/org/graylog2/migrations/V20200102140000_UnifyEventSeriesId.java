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
package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.aggregation.AggregationConditions;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class V20200102140000_UnifyEventSeriesId extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20200102140000_UnifyEventSeriesId.class);

    private final ClusterConfigService clusterConfigService;
    private final DBEventDefinitionService eventDefinitionService;
    private final ObjectMapperProvider objectMapperProvider;

    @Inject
    public V20200102140000_UnifyEventSeriesId(ClusterConfigService clusterConfigService, DBEventDefinitionService eventDefinitionService, ObjectMapperProvider objectMapperProvider) {
        this.clusterConfigService = clusterConfigService;
        this.eventDefinitionService = eventDefinitionService;
        this.objectMapperProvider = objectMapperProvider;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-01-02T14:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }
        final List<EventDefinitionDto> changedEventDefinitions;
        try (final Stream<EventDefinitionDto> dtoStream = eventDefinitionService.streamAll()) {
            changedEventDefinitions = dtoStream.map(this::unifySeriesId).filter(Objects::nonNull).collect(Collectors.toList());
        }
        for (final EventDefinitionDto changedDto : changedEventDefinitions) {
            LOG.info("Unified series Id for EventDefinition <{}>", changedDto.id());
            eventDefinitionService.save(changedDto);
        }
        clusterConfigService.write(MigrationCompleted.create());
    }

    private EventDefinitionDto unifySeriesId(EventDefinitionDto dto) {
        if (!dto.config().type().equals(AggregationEventProcessorConfig.TYPE_NAME)) {
            return null;
        }
        final AggregationEventProcessorConfig config = (AggregationEventProcessorConfig) dto.config();
        if (config.series().isEmpty()) {
            return null;
        }
        final ObjectMapper objectMapper = objectMapperProvider.get();
        final AggregationEventProcessorConfig.Builder configBuilder = config.toBuilder();
        final Map<String, String> refMap = new HashMap<>();

        configBuilder.series(config.series().stream().map(s -> {
            final String newId = s.function().toSeriesId(s.field());
            refMap.put(s.id(), newId);
            return s.toBuilder()
                    .id(newId)
                    .build();
        }).collect(Collectors.toList()));

        // convert conditions to json, fix them and convert back to POJO
        final JsonNode conditionsJson = objectMapper.valueToTree(config.conditions());
        convertConditions(dto.id(), refMap, conditionsJson);
        final AggregationConditions convertedConditions = objectMapper.convertValue(conditionsJson, AggregationConditions.class);
        configBuilder.conditions(convertedConditions);

        return dto.toBuilder().config(configBuilder.build()).build();
    }

    private void convertConditions(String eventId, Map<String, String> changedIds, final JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            final ObjectNode objectNode = (ObjectNode) jsonNode;
            if (objectNode.get("expr") != null && objectNode.get("expr").asText().equals("number-ref") ) {
                final String newRef = changedIds.get(objectNode.get("ref").asText());
                if (newRef == null) {
                    throw new RuntimeException(String.format(Locale.US,
                            "Could not resolve new ref for condition on EventDefinition <%s>. oldref <%s> refMap <%s>",
                            eventId, objectNode.get("ref"), changedIds));
                }
                objectNode.put("ref", newRef);
            }
        }
        // recurse into tree
        jsonNode.fields().forEachRemaining(f -> {
            final JsonNode value = f.getValue();
            if (value.isContainerNode()) {
                convertConditions(eventId, changedIds, value);
            }
        });
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonCreator
        public static MigrationCompleted create() {
            return new AutoValue_V20200102140000_UnifyEventSeriesId_MigrationCompleted();
        }
    }
}
