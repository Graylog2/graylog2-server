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
package org.graylog.events.processor.mongodb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.graph.MutableGraph;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bson.Document;
import org.graylog.events.contentpack.entities.EventProcessorConfigEntity;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.events.processor.EventProcessorSchedulerConfig;
import org.graylog.scheduler.JobSchedule;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.security.UserContext;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.rest.ValidationResult;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@AutoValue
@JsonTypeName(MongoDBEventProcessorConfig.TYPE_NAME)
@JsonDeserialize(builder = MongoDBEventProcessorConfig.Builder.class)
public abstract class MongoDBEventProcessorConfig implements EventProcessorConfig {
    public static final String TYPE_NAME = "mongodb-v1";

    private static final String FIELD_COLLECTION_NAME = "collection_name";
    private static final String FIELD_AGGREGATION_PIPELINE = "aggregation_pipeline";
    private static final String FIELD_TIMESTAMP_FIELD = "timestamp_field";
    private static final String FIELD_SEARCH_WITHIN_SECONDS = "search_within_seconds";
    private static final String FIELD_EXECUTE_EVERY_SECONDS = "execute_every_seconds";

    @JsonProperty(FIELD_COLLECTION_NAME)
    public abstract String collectionName();

    @JsonProperty(FIELD_AGGREGATION_PIPELINE)
    public abstract String aggregationPipeline();

    @JsonProperty(FIELD_TIMESTAMP_FIELD)
    public abstract String timestampField();

    // Ignored for START_OF_DAY mode
    @JsonProperty(FIELD_SEARCH_WITHIN_SECONDS)
    public abstract long searchWithinSeconds();

    @JsonProperty(FIELD_EXECUTE_EVERY_SECONDS)
    public abstract long executeEverySeconds();

    @Override
    public Set<String> requiredPermissions() {
        // MongoDB event processor doesn't require specific stream permissions
        return Collections.emptySet();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @Override
    public Optional<EventProcessorSchedulerConfig> toJobSchedulerConfig(EventDefinition eventDefinition, JobSchedulerClock clock) {
        return createSchedulerConfig(eventDefinition, clock, searchWithinSeconds(), executeEverySeconds());
    }

    /**
     * Helper method to create scheduler config from time parameters.
     * Can be reused by other MongoDB-based event processor configs.
     */
    public static Optional<EventProcessorSchedulerConfig> createSchedulerConfig(EventDefinition eventDefinition,
                                                                                JobSchedulerClock clock,
                                                                                long searchWithinSeconds,
                                                                                long executeEverySeconds) {
        return createSchedulerConfig(eventDefinition, clock, searchWithinSeconds, executeEverySeconds, false);
    }

    /**
     * Helper method to create scheduler config from time parameters with optional catch-up control.
     * Can be reused by other MongoDB-based event processor configs.
     *
     * @param enableCatchup if true, enables catch-up behavior when processor falls behind; if false, disables catch-up
     */
    public static Optional<EventProcessorSchedulerConfig> createSchedulerConfig(EventDefinition eventDefinition,
                                                                                JobSchedulerClock clock,
                                                                                long searchWithinSeconds,
                                                                                long executeEverySeconds,
                                                                                boolean enableCatchup) {
        final DateTime now = clock.nowUTC();

        // Convert seconds to milliseconds for scheduler
        final long searchWithinMs = searchWithinSeconds * 1000;
        final long executeEveryMs = executeEverySeconds * 1000;

        // Create interval-based schedule
        final JobSchedule schedule = IntervalJobSchedule.builder()
                .interval(executeEveryMs)
                .unit(TimeUnit.MILLISECONDS)
                .build();

        // Initial timerange for first execution
        final AbsoluteRange timerange = AbsoluteRange.create(now.minus(searchWithinMs), now);

        final EventProcessorExecutionJob.Config jobDefinitionConfig = EventProcessorExecutionJob.Config.builder()
                .eventDefinitionId(eventDefinition.id())
                .processingWindowSize(searchWithinMs)
                .processingHopSize(executeEveryMs)
                .enableCatchup(enableCatchup)  // Disable catch-up for MongoDB-based processors by default
                .parameters(MongoDBEventProcessorParameters.builder()
                        .timerange(timerange)
                        .build())
                .build();

        return Optional.of(EventProcessorSchedulerConfig.create(jobDefinitionConfig, schedule));
    }

    @AutoValue.Builder
    public abstract static class Builder implements EventProcessorConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_MongoDBEventProcessorConfig.Builder()
                    .type(TYPE_NAME)
                    .timestampField("bucket")     // Default timestamp field
                    .searchWithinSeconds(60)      // Default: 1 minute
                    .executeEverySeconds(60);     // Default: 1 minute
        }

        @JsonProperty(FIELD_COLLECTION_NAME)
        public abstract Builder collectionName(String collectionName);

        @JsonProperty(FIELD_AGGREGATION_PIPELINE)
        public abstract Builder aggregationPipeline(String aggregationPipeline);

        @JsonProperty(FIELD_TIMESTAMP_FIELD)
        public abstract Builder timestampField(String timestampField);

        @JsonProperty(FIELD_SEARCH_WITHIN_SECONDS)
        public abstract Builder searchWithinSeconds(long searchWithinSeconds);

        @JsonProperty(FIELD_EXECUTE_EVERY_SECONDS)
        public abstract Builder executeEverySeconds(long executeEverySeconds);

        public abstract MongoDBEventProcessorConfig build();
    }

    @Override
    public ValidationResult validate(UserContext userContext) {
        final ValidationResult validationResult = new ValidationResult();

        // Validate collection name
        if (collectionName() == null || collectionName().trim().isEmpty()) {
            validationResult.addError(FIELD_COLLECTION_NAME,
                    "Collection name is required");
        }

        // Validate aggregation pipeline JSON
        if (aggregationPipeline() == null || aggregationPipeline().trim().isEmpty()) {
            validationResult.addError(FIELD_AGGREGATION_PIPELINE,
                    "Aggregation pipeline is required");
        } else {
            try {
                JsonArray pipeline = JsonParser.parseString(aggregationPipeline()).getAsJsonArray();
                if (pipeline.isEmpty()) {
                    validationResult.addError(FIELD_AGGREGATION_PIPELINE,
                            "Aggregation pipeline must have at least one stage");
                }
                // Validate each stage is valid BSON
                for (JsonElement stage : pipeline) {
                    Document.parse(stage.toString());
                }
            } catch (Exception e) {
                validationResult.addError(FIELD_AGGREGATION_PIPELINE,
                        "Invalid aggregation pipeline JSON: " + e.getMessage());
            }
        }

        // Validate timestamp field
        if (timestampField() == null || timestampField().trim().isEmpty()) {
            validationResult.addError(FIELD_TIMESTAMP_FIELD,
                    "Timestamp field must not be empty");
        }

        // Validate search window
        if (searchWithinSeconds() <= 0) {
            validationResult.addError(FIELD_SEARCH_WITHIN_SECONDS,
                    "Search window must be greater than 0");
        }

        // Validate execution interval
        if (executeEverySeconds() <= 0) {
            validationResult.addError(FIELD_EXECUTE_EVERY_SECONDS,
                    "Execution interval must be greater than 0");
        }

        return validationResult;
    }

    @Override
    public EventProcessorConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        // Content pack support can be implemented later if needed
        return null;
    }

    @Override
    public void resolveNativeEntity(EntityDescriptor entityDescriptor, MutableGraph<EntityDescriptor> mutableGraph) {
        // No external dependencies to resolve
    }
}
