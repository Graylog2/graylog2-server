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
import com.google.auto.value.AutoValue;
import jakarta.inject.Inject;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePoller;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SetIndexReadOnlyAndCalculateRangeJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(SetIndexReadOnlyAndCalculateRangeJob.class);

    public static final String TYPE_NAME = "set-index-read-only-and-calculate-range-v1";
    public static final String JOB_ID = "662a07ad68699718ec07b159";
    public static final JobDefinitionDto DEFINITION_INSTANCE = JobDefinitionDto.builder()
            .id(JOB_ID) // This is a system entity and the ID MUST NOT change!
            .title("Create Index Range")
            .description("Creates Index Range for a single index.")
            .config(SetIndexReadOnlyAndCalculateRangeJob.Config.empty())
            .build();

    private final Indices indices;
    private final IndexRangeService indexRangeService;
    private final IndexJobsService indexJobsService;
    private final IndexSetRegistry indexSetRegistry;
    private final ActivityWriter activityWriter;
    private final IndexFieldTypesService indexFieldTypesService;
    private final IndexFieldTypePoller indexFieldTypePoller;

    @Inject
    public SetIndexReadOnlyAndCalculateRangeJob(final Indices indices,
                                                final IndexRangeService indexRangeService,
                                                final IndexJobsService indexJobsService,
                                                final ActivityWriter activityWriter,
                                                final IndexSetRegistry indexSetRegistry,
                                                final IndexFieldTypePoller indexFieldTypePoller,
                                                final IndexFieldTypesService indexFieldTypesService) {
        this.indices = indices;
        this.indexRangeService = indexRangeService;
        this.indexJobsService = indexJobsService;
        this.activityWriter = activityWriter;
        this.indexSetRegistry = indexSetRegistry;
        this.indexFieldTypePoller = indexFieldTypePoller;
        this.indexFieldTypesService = indexFieldTypesService;
    }

    public interface Factory extends Job.Factory<SetIndexReadOnlyAndCalculateRangeJob> {
        @Override
        SetIndexReadOnlyAndCalculateRangeJob create(JobDefinitionDto jobDefinition);
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        final JobTriggerDto trigger = ctx.trigger();
        final SetIndexReadOnlyAndCalculateRangeJob.Data jobData = trigger.data()
                .map(d -> (SetIndexReadOnlyAndCalculateRangeJob.Data) d)
                .orElseThrow(() -> new IllegalArgumentException("SetIndexReadOnlyAndCalculateRangeJob job data not found"));

        final var indexName = jobData.indexName();

        if (!indices.exists(indexName)) {
            LOG.debug("Not running job for deleted index <{}>", indexName);
            return JobTriggerUpdate.withStatusAndNoNextTime(JobTriggerStatus.COMPLETE);
        }
        if (indices.isClosed(indexName)) {
            LOG.debug("Not running job for closed index <{}>", indexName);
            return JobTriggerUpdate.withStatusAndNoNextTime(JobTriggerStatus.COMPLETE);
        }
        setReadonly(indexName);
        indexRangeService.calculateRangeAndSave(indexName);

        // Update field type information again to make sure we got the latest state
        indexSetRegistry.getForIndex(indexName)
                .ifPresent(indexSet -> {
                    indexFieldTypePoller.pollIndex(indexName, indexSet.getConfig().id())
                            .ifPresent(indexFieldTypesService::upsert);
                });

        return JobTriggerUpdate.withStatusAndNoNextTime(JobTriggerStatus.COMPLETE);
    }

    public void setReadonly(final String indexName) {
        final Optional<IndexSet> indexSet = indexSetRegistry.getForIndex(indexName);

        if (indexSet.isEmpty()) {
            LOG.error("Couldn't find index set for index <{}>", indexName);
            return;
        }

        LOG.info("Flushing old index <{}>.", indexName);
        indices.flush(indexName);

        // Record the time an index was set read-only.
        // We call this the "closing date" because it denotes when we stopped writing to it.
        indices.setClosingDate(indexName, Tools.nowUTC());

        LOG.info("Setting old index <{}> to read-only.", indexName);
        indices.setReadOnly(indexName);

        activityWriter.write(new Activity("Flushed and set <" + indexName + "> to read-only.", SetIndexReadOnlyAndCalculateRangeJob.class));

        if (!indexSet.get().getConfig().indexOptimizationDisabled()) {
            indexJobsService.submitOptimizeIndexJob(indexName, indexSet.get().getConfig().indexOptimizationMaxNumSegments());
        }
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements JobDefinitionConfig {
        @JsonCreator
        public static Config create(@JsonProperty("type") String type) {
            return new AutoValue_SetIndexReadOnlyAndCalculateRangeJob_Config(type);
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

        @JsonProperty(FIELD_INDEX_NAME)
        public abstract String indexName();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public static abstract class Builder implements JobTriggerData.Builder<Data.Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_SetIndexReadOnlyAndCalculateRangeJob_Data.Builder()
                        .type(TYPE_NAME);
            }

            @JsonProperty(FIELD_INDEX_NAME)
            public abstract Builder indexName(String indexName);

            abstract Data autoBuild();

            public Data build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }
}
