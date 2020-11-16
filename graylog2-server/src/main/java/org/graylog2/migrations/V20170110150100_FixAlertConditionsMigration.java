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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Updates.set;

public class V20170110150100_FixAlertConditionsMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20170110150100_FixAlertConditionsMigration.class);

    private static final String FIELD_ID = "_id";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String FIELD_ALERT_CONDITIONS = "alert_conditions";
    private static final String FIELD_ALERT_CONDITIONS_ID = "alert_conditions.id";
    private static final String ALERT_CONDITIONS_PARAMETERS_PREFIX = "alert_conditions.$.parameters.";

    private final MongoCollection<Document> collection;
    private final ClusterConfigServiceImpl clusterConfigService;

    @Inject
    public V20170110150100_FixAlertConditionsMigration(final MongoConnection mongoConnection,
                                                       final ClusterConfigServiceImpl clusterConfigService) {
        this.collection = mongoConnection.getMongoDatabase().getCollection("streams");
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2017-01-10T15:01:00Z");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already done.");
            return;
        }

        final ImmutableSet.Builder<String> modifiedStreams = ImmutableSet.builder();
        final ImmutableSet.Builder<String> modifiedAlertConditions = ImmutableSet.builder();

        for (Document document : collection.find().sort(ascending(FIELD_CREATED_AT))) {

            final String streamId = document.getObjectId(FIELD_ID).toHexString();

            if (!document.containsKey(FIELD_ALERT_CONDITIONS)) {
                continue;
            }

            final List<Document> alertConditions = (List<Document>) document.get(FIELD_ALERT_CONDITIONS);

            // Need to check if the following fields are integers:
            //
            // FieldContentValue: grace, backlog
            // FieldValue:        grace, backlog, time, threshold
            // MessageCount:      grace, backlog, time, threshold
            final Set<String> intFields = ImmutableSet.of("grace", "backlog", "time", "threshold");

            for (Document alertCondition : alertConditions) {
                final String alertConditionId = alertCondition.get("id", String.class);
                final String alertConditionTitle = alertCondition.get("title", String.class);
                final Document parameters = alertCondition.get("parameters", Document.class);

                for (String field : intFields) {
                    final Object fieldValue = parameters.get(field);

                    // No need to convert anything if the field does not exist or is already an integer
                    if (fieldValue == null || fieldValue instanceof Integer) {
                        continue;
                    }

                    if (!(fieldValue instanceof String)) {
                        LOG.warn("Field <{}> in alert condition <{}> ({}) of stream <{}> is not a string but a <{}>, not trying to convert it!",
                                field, alertConditionId, alertConditionTitle, streamId,
                                fieldValue.getClass().getCanonicalName());
                        continue;
                    }

                    final String stringValue = parameters.get(field, String.class);
                    final Integer intValue = Ints.tryParse(stringValue);

                    LOG.info("Converting value for field <{}> from string to integer in alert condition <{}> ({}) of stream <{}>",
                            field, alertConditionId, alertConditionTitle, streamId);

                    if (intValue == null) {
                        LOG.error("Unable to parse \"{}\" into integer!", fieldValue);
                    }

                    final UpdateResult result = collection.updateOne(eq(FIELD_ALERT_CONDITIONS_ID, alertConditionId), set(ALERT_CONDITIONS_PARAMETERS_PREFIX + field, intValue));

                    // Use UpdateResult#getMatchedCount() instead of #getModifiedCount() to make it work on MongoDB 2.4
                    if (result.getMatchedCount() > 0) {
                        modifiedStreams.add(streamId);
                        modifiedAlertConditions.add(alertConditionId);
                    } else {
                        LOG.warn("No document modified for alert condition <{}> ({})", alertConditionId, alertConditionTitle);
                    }
                }
            }
        }

        clusterConfigService.write(MigrationCompleted.create(modifiedStreams.build(), modifiedAlertConditions.build()));
    }

    @AutoValue
    public static abstract class MigrationCompleted {
        @JsonProperty("stream_ids")
        public abstract Set<String> streamIds();

        @JsonProperty("alert_condition_ids")
        public abstract Set<String> alertConditionIds();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("stream_ids") Set<String> streamIds,
                                                @JsonProperty("alert_condition_ids") Set<String> alertConditionIds) {
            return new AutoValue_V20170110150100_FixAlertConditionsMigration_MigrationCompleted(streamIds, alertConditionIds);
        }
    }
}
