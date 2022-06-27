package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
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
        final MigrationCompleted migrationCompleted = clusterConfigService.getOrDefault(MigrationCompleted.class, MigrationCompleted.createEmpty());

        // Run this more than once in case old cluster nodes are still writing triggerDTOs with empty types
        if (migrationCompleted.executionCount() >= 3) {
            return;
        }

        final MongoCollection<Document> jobTriggers = mongoConnection.getMongoDatabase().getCollection(DBJobTriggerService.COLLECTION_NAME);

        // Duplicate the type field from the JobDefinition config to their JobTriggers
        // Yes, this is what it takes to perform a join in MongoDB  ¯\_(ツ)_/¯
        final AggregateIterable<Document> query = jobTriggers.aggregate(
                ImmutableList.of(
                        Aggregates.match(Filters.exists(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, false)),
                        Aggregates.project(Projections.computed("job_definition_id", new Document("$toObjectId", "$job_definition_id"))),
                        Aggregates.lookup(DBJobDefinitionService.COLLECTION_NAME, "job_definition_id", "_id", "job_definition"),
                        Aggregates.project(Projections.computed("job_definition", new Document("$arrayElemAt", Arrays.asList("$job_definition", 0)))),
                        Aggregates.project(Projections.computed("job_type", "$job_definition.config.type"))
                )
        );

        List<UpdateOneModel<Document>> typeUpdate = Streams.stream(query).map(d -> new UpdateOneModel<Document>(
                Filters.eq("_id", d.getObjectId("_id")),
                Updates.set(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, d.getString("job_type")))).collect(Collectors.toList());

        long modifiedCount = 0;
        if (!typeUpdate.isEmpty()) {
            modifiedCount = jobTriggers.bulkWrite(typeUpdate).getModifiedCount();

            LOG.info("Added type field to <{}> JobTriggers", modifiedCount);
        }

        clusterConfigService.write(MigrationCompleted.
                create(migrationCompleted.convertedTriggersCount() + modifiedCount,
                        migrationCompleted.executionCount() + 1));
    }

    @AutoValue
    public static abstract class MigrationCompleted {
        @JsonProperty("converted_triggers_count")
        public abstract long convertedTriggersCount();

        @JsonProperty("execution_count")
        public abstract int executionCount();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("converted_triggers_count") long convertedTriggersCount, @JsonProperty("execution_count") int executionCount) {
            return new AutoValue_V20220623125450_AddJobTypeToJobTrigger_MigrationCompleted(convertedTriggersCount, executionCount);
        }

        public static MigrationCompleted createEmpty() {
            return create(0, 0);
        }
    }
}
