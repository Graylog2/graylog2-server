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
import com.google.auto.value.AutoValue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class V20200102140000_UnifyEventSeriesId extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20200102140000_UnifyEventSeriesId.class);

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> eventDefinitions;

    @Inject
    public V20200102140000_UnifyEventSeriesId(ClusterConfigService clusterConfigService,
                                              MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.eventDefinitions = mongoConnection.getMongoDatabase().getCollection("event_definitions");
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
        var changedEventDefinitions = StreamSupport.stream(eventDefinitions.find().spliterator(), false)
                .map(this::unifySeriesId)
                .filter(Objects::nonNull)
                .toList();

        for (final Document changedEventDefinition : changedEventDefinitions) {
            var id = changedEventDefinition.getObjectId("_id");
            LOG.info("Unified series Id for EventDefinition <{}>", id);
            eventDefinitions.replaceOne(Filters.eq("_id", id), changedEventDefinition);
        }
        clusterConfigService.write(MigrationCompleted.create());
    }

    private Document unifySeriesId(Document dto) {
        final Document config = dto.get("config", Document.class);
        if (!config.getString("type").equals(AggregationEventProcessorConfig.TYPE_NAME)) {
            return null;
        }

        var series = config.getList("series", Document.class);
        if (series.isEmpty()) {
            return null;
        }
        final Map<String, String> refMap = new HashMap<>(series.size());

        series.forEach(s -> {
            var id = s.getString("id");
            var name = s.getString("function");
            var field = Optional.ofNullable(s.getString("field"));
            var newId = toSeriesId(name, field);
            s.put("id", newId);
            refMap.put(id, newId);
        });

        // convert conditions to json, fix them and convert back to POJO
        final Document conditions = config.getEmbedded(List.of("conditions", "expression"), Document.class);
        var id = config.getObjectId("_id");
        convertConditions(id, refMap, conditions);

        return dto;
    }

    private String toSeriesId(String name, Optional<String> field) {
        return String.format(Locale.US, "%s-%s", name.toLowerCase(Locale.US), field.orElse(""));
    }

    private void convertConditions(ObjectId eventId, Map<String, String> changedIds, Document conditions) {
        Optional.ofNullable(conditions.getString("expr"))
                .filter(expr -> expr.equals("number-ref"))
                .map((expr) -> Optional.ofNullable(changedIds.get(conditions.getString("ref"))).orElseThrow(() -> new RuntimeException(String.format(Locale.US,
                        "Could not resolve new ref for condition on EventDefinition <%s>. oldref <%s> refMap <%s>",
                        eventId, conditions.getString("ref"), changedIds)))
                ).ifPresent(newRef -> conditions.put("ref", newRef));

        Optional.ofNullable(conditions.get("left", Document.class))
                .ifPresent(left -> convertConditions(eventId, changedIds, left));
        Optional.ofNullable(conditions.get("right", Document.class))
                .ifPresent(right -> convertConditions(eventId, changedIds, right));
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
