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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@AutoValue
@JsonTypeName(MongoDBEventProcessorConfig.TYPE_NAME)
@JsonDeserialize(builder = MongoDBEventProcessorConfig.Builder.class)
public abstract class MongoDBEventProcessorConfig implements EventProcessorConfig {
    public static final String TYPE_NAME = "mongodb-v1";

    private static final String FIELD_AGGREGATION_PIPELINE = "aggregation_pipeline";
    private static final String FIELD_TIMESTAMP_FIELD = "timestamp_field";
    private static final String FIELD_SEARCH_WITHIN_MS = "search_within_ms";
    private static final String FIELD_EXECUTE_EVERY_MS = "execute_every_ms";

    @JsonProperty(FIELD_AGGREGATION_PIPELINE)
    public abstract String aggregationPipeline();

    @JsonProperty(FIELD_TIMESTAMP_FIELD)
    public abstract String timestampField();

    @JsonProperty(FIELD_SEARCH_WITHIN_MS)
    public abstract long searchWithinMs();

    @JsonProperty(FIELD_EXECUTE_EVERY_MS)
    public abstract long executeEveryMs();

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
        final DateTime now = clock.nowUTC();

        // Create interval-based schedule
        final JobSchedule schedule = IntervalJobSchedule.builder()
                .interval(executeEveryMs())
                .unit(TimeUnit.MILLISECONDS)
                .build();

        // Initial timerange for first execution
        final AbsoluteRange timerange = AbsoluteRange.create(now.minus(searchWithinMs()), now);

        final EventProcessorExecutionJob.Config jobDefinitionConfig = EventProcessorExecutionJob.Config.builder()
                .eventDefinitionId(eventDefinition.id())
                .processingWindowSize(searchWithinMs())
                .processingHopSize(executeEveryMs())
                .parameters(MongoDBEventProcessorParameters.builder()
                        .timerange(timerange)
                        .build())
                .build();

        return Optional.of(EventProcessorSchedulerConfig.create(jobDefinitionConfig, schedule));
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventProcessorConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_MongoDBEventProcessorConfig.Builder()
                    .type(TYPE_NAME)
                    .timestampField("bucket")  // Default timestamp field for TrafficDto
                    .searchWithinMs(60000)     // Default: 1 minute
                    .executeEveryMs(60000);    // Default: 1 minute
        }

        @JsonProperty(FIELD_AGGREGATION_PIPELINE)
        public abstract Builder aggregationPipeline(String aggregationPipeline);

        @JsonProperty(FIELD_TIMESTAMP_FIELD)
        public abstract Builder timestampField(String timestampField);

        @JsonProperty(FIELD_SEARCH_WITHIN_MS)
        public abstract Builder searchWithinMs(long searchWithinMs);

        @JsonProperty(FIELD_EXECUTE_EVERY_MS)
        public abstract Builder executeEveryMs(long executeEveryMs);

        public abstract MongoDBEventProcessorConfig build();
    }

    @Override
    public ValidationResult validate(UserContext userContext) {
        final ValidationResult validationResult = new ValidationResult();

        // Validate aggregation pipeline JSON
        if (aggregationPipeline() == null || aggregationPipeline().trim().isEmpty()) {
            validationResult.addError(FIELD_AGGREGATION_PIPELINE,
                    "Aggregation pipeline is required");
        } else {
            try {
                JsonArray pipeline = JsonParser.parseString(aggregationPipeline()).getAsJsonArray();
                if (pipeline.size() == 0) {
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
        if (searchWithinMs() <= 0) {
            validationResult.addError(FIELD_SEARCH_WITHIN_MS,
                    "Search window must be greater than 0");
        }

        // Validate execution interval
        if (executeEveryMs() <= 0) {
            validationResult.addError(FIELD_EXECUTE_EVERY_MS,
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
