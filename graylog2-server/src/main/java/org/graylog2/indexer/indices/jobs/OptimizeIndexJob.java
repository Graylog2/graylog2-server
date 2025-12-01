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
package org.graylog2.indexer.indices.jobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.joschi.jadconfig.util.Duration;
import com.google.auto.value.AutoValue;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizeIndexJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(OptimizeIndexJob.class);

    public static final String TYPE_NAME = "optimize-index-v1";
    public static final String JOB_ID = "662a07ad68699718ec07b171";
    public static final JobDefinitionDto DEFINITION_INSTANCE = JobDefinitionDto.builder()
            .id(JOB_ID) // This is a system entity and the ID MUST NOT change!
            .title("Optimize Index")
            .description("Schedules index optimization on the Indexer.")
            .config(CreateNewSingleIndexRangeJob.Config.empty())
            .build();

    private final Indices indices;
    private final ActivityWriter activityWriter;

    private final Duration indexOptimizationTimeout;
    // TODO: check concurrency in new JobScheduler
    private final int indexOptimizationJobs;

    @Inject
    public OptimizeIndexJob(final Indices indices,
                            final ActivityWriter activityWriter,
                            final @Named("elasticsearch_index_optimization_timeout") Duration indexOptimizationTimeout,
                            final @Named("elasticsearch_index_optimization_jobs") int indexOptimizationJobs) {
        this.indices = indices;
        this.activityWriter = activityWriter;
        this.indexOptimizationTimeout = indexOptimizationTimeout;
        this.indexOptimizationJobs = indexOptimizationJobs;
    }

    public interface Factory extends Job.Factory<OptimizeIndexJob> {
        @Override
        OptimizeIndexJob create(JobDefinitionDto jobDefinition);
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        final JobTriggerDto trigger = ctx.trigger();
        final Data jobData = trigger.data()
                .map(d -> (Data) d)
                .orElseThrow(() -> new IllegalArgumentException("OptimizeIndexJob job data not found"));

        final var index = jobData.indexName();
        final var maxNumSegments = jobData.maxNumSegments();

        if (!indices.exists(index)) {
            LOG.debug("Not running job for deleted index <{}>", index);
            return JobTriggerUpdate.withStatusAndNoNextTime(JobTriggerStatus.COMPLETE);
        }
        if (indices.isClosed(index)) {
            LOG.debug("Not running job for closed index <{}>", index);
            return JobTriggerUpdate.withStatusAndNoNextTime(JobTriggerStatus.COMPLETE);
        }

        String msg = "Optimizing index <" + index + ">.";
        activityWriter.write(new Activity(msg, OptimizeIndexJob.class));
        LOG.info(msg);

        indices.optimizeIndex(index, maxNumSegments, indexOptimizationTimeout);
        return JobTriggerUpdate.withStatusAndNoNextTime(JobTriggerStatus.COMPLETE);
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements JobDefinitionConfig {
        @JsonCreator
        public static Config create(@JsonProperty("type") String type) {
            return new AutoValue_OptimizeIndexJob_Config(type);
        }

        public static Config empty() {
            return create(TYPE_NAME);
        }
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    @JsonDeserialize(builder = Data.Builder.class)
    public static abstract class Data implements JobTriggerData {
        private static final String FIELD_INDEX_NAME = "index_name";
        private static final String FIELD_MAX_NUM_SEGMENTS_NAME = "max_num_segments";

        @JsonProperty(FIELD_INDEX_NAME)
        public abstract String indexName();

        @JsonProperty(FIELD_MAX_NUM_SEGMENTS_NAME)
        public abstract Integer maxNumSegments();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public static abstract class Builder implements JobTriggerData.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_OptimizeIndexJob_Data.Builder()
                        .type(TYPE_NAME);
            }

            @JsonProperty(FIELD_INDEX_NAME)
            public abstract Builder indexName(String indexName);

            @JsonProperty(FIELD_MAX_NUM_SEGMENTS_NAME)
            public abstract Builder maxNumSegments(Integer maxNumSegments);

            abstract Data autoBuild();

            public Data build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }
}

