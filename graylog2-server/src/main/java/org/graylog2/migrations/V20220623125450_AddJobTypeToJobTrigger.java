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
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class V20220623125450_AddJobTypeToJobTrigger extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20220623125450_AddJobTypeToJobTrigger.class);


    private final MongoConnection mongoConnection;

    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20220623125450_AddJobTypeToJobTrigger(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.mongoConnection = mongoConnection;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-06-23T12:54:50Z");
    }

    @Override
    public void upgrade() {
        final MigrationStatus migrationStatus = clusterConfigService.getOrDefault(MigrationStatus.class, MigrationStatus.createEmpty());

        final Stopwatch started = Stopwatch.createStarted();

        final MongoCollection<Document> jobTriggers = mongoConnection.getMongoDatabase().getCollection(DBJobTriggerService.COLLECTION_NAME);
        final MongoCollection<Document> jobDefinitions = mongoConnection.getMongoDatabase().getCollection(DBJobDefinitionService.COLLECTION_NAME);


        // Duplicate the type field from the JobDefinition config to their JobTriggers

        // We cannot use aggregations because Mongo 3.6 does not support $toString or $toObjectId
        final Map<ObjectId, String> typeMap = Streams.stream(jobDefinitions.find())
                .collect(Collectors.toMap(d -> d.getObjectId("_id"), d -> d.getEmbedded(ImmutableList.of("config", "type"), String.class)));

        final FindIterable<Document> query = jobTriggers.find(Filters.exists(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, false));

        List<UpdateOneModel<Document>> typeUpdate = Streams.stream(query).map(d -> new UpdateOneModel<Document>(
                Filters.eq("_id", d.getObjectId("_id")),
                Updates.set(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, typeMap.get(d.getObjectId("_id"))))).collect(Collectors.toList());

        long modifiedCount = 0;
        if (!typeUpdate.isEmpty()) {
            modifiedCount = jobTriggers.bulkWrite(typeUpdate).getModifiedCount();

            LOG.info("Added type field to <{}> JobTriggers", modifiedCount);
        }
        final long elapsed = started.stop().elapsed(TimeUnit.SECONDS);
        if (elapsed > 60) {
            LOG.warn("Migration ran longer than expected! Took <{}> seconds", elapsed);
        }

        clusterConfigService.write(MigrationStatus.
                create(migrationStatus.convertedTriggersCount() + modifiedCount,
                        migrationStatus.executionCount() + 1));
    }

    @AutoValue
    public static abstract class MigrationStatus {
        @JsonProperty("converted_triggers_count")
        public abstract long convertedTriggersCount();

        @JsonProperty("execution_count")
        public abstract int executionCount();

        @JsonCreator
        public static MigrationStatus create(@JsonProperty("converted_triggers_count") long convertedTriggersCount, @JsonProperty("execution_count") int executionCount) {
            return new AutoValue_V20220623125450_AddJobTypeToJobTrigger_MigrationStatus(convertedTriggersCount, executionCount);
        }

        public static MigrationStatus createEmpty() {
            return create(0, 0);
        }
    }
}
