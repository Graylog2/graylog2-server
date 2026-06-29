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
package org.graylog2.indexer.indices;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-shot job that reindexes a single outdated index into a format compatible with the next major OpenSearch version.
 * The actual work is delegated to {@link OutdatedIndexService#reindex(String, boolean)} and runs once on a scheduler
 * worker thread, so the triggering REST request returns immediately instead of blocking for the whole reindex.
 */
public class ReindexOutdatedIndexJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(ReindexOutdatedIndexJob.class);

    public static final String TYPE_NAME = "reindex-outdated-index-v1";

    /**
     * Built-in singleton job definition. The ID must remain stable across deployments so triggers can reference it.
     */
    public static final JobDefinitionDto DEFINITION_INSTANCE = JobDefinitionDto.builder()
            .id("6850b1c2d3e4f5a607182930")
            .title("Reindex outdated index")
            .description("Built-in job definition for reindexing outdated indices to the current major OpenSearch version.")
            .config(Config.builder().build())
            .build();

    private final OutdatedIndexService outdatedIndexService;

    public interface Factory extends Job.Factory<ReindexOutdatedIndexJob> {
        @Override
        ReindexOutdatedIndexJob create(JobDefinitionDto jobDefinition);
    }

    @Inject
    public ReindexOutdatedIndexJob(@Assisted JobDefinitionDto jobDefinition, OutdatedIndexService outdatedIndexService) {
        this.outdatedIndexService = outdatedIndexService;
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        final Data data = ctx.trigger().data()
                .filter(Data.class::isInstance)
                .map(Data.class::cast)
                .orElseThrow(() -> new JobExecutionException(
                        "Missing data for reindex outdated index job",
                        ctx.trigger(),
                        JobTriggerUpdate.withError(ctx.trigger())));

        try {
            LOG.info("Reindexing outdated index {} (withReplicas={}), triggered by {}",
                    data.index(), data.withReplicas(), data.triggeredBy());
            outdatedIndexService.reindex(data.index(), data.withReplicas());
            LOG.info("Finished reindexing outdated index {}", data.index());
            // No next time -> trigger is marked COMPLETE. Keep the data so callers can inspect the finished trigger.
            return JobTriggerUpdate.builder().nextTime(null).data(data).build();
        } catch (Exception e) {
            LOG.error("Reindexing outdated index {} failed", data.index(), e);
            final Data failed = data.toBuilder().lastError(e.getMessage()).build();
            return JobTriggerUpdate.builder()
                    .status(JobTriggerStatus.ERROR)
                    .data(failed)
                    .nextTime(null)
                    .build();
        }
    }

    @AutoValue
    @JsonTypeName(ReindexOutdatedIndexJob.TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public abstract static class Config implements JobDefinitionConfig {

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder implements JobDefinitionConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_ReindexOutdatedIndexJob_Config.Builder().type(TYPE_NAME);
            }

            abstract Config autoBuild();

            public Config build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }

    @AutoValue
    @JsonTypeName(ReindexOutdatedIndexJob.TYPE_NAME)
    @JsonDeserialize(builder = Data.Builder.class)
    public abstract static class Data implements JobTriggerData {

        public static final String FIELD_INDEX = "index";
        public static final String FIELD_WITH_REPLICAS = "with_replicas";
        public static final String FIELD_TRIGGERED_BY = "triggered_by";
        public static final String FIELD_LAST_ERROR = "last_error";

        @JsonProperty(FIELD_INDEX)
        public abstract String index();

        @JsonProperty(FIELD_WITH_REPLICAS)
        public abstract boolean withReplicas();

        @JsonProperty(FIELD_TRIGGERED_BY)
        public abstract String triggeredBy();

        @Nullable
        @JsonProperty(FIELD_LAST_ERROR)
        public abstract String lastError();

        public abstract Builder toBuilder();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder implements JobTriggerData.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_ReindexOutdatedIndexJob_Data.Builder().type(TYPE_NAME);
            }

            @JsonProperty(FIELD_INDEX)
            public abstract Builder index(String index);

            @JsonProperty(FIELD_WITH_REPLICAS)
            public abstract Builder withReplicas(boolean withReplicas);

            @JsonProperty(FIELD_TRIGGERED_BY)
            public abstract Builder triggeredBy(String triggeredBy);

            @JsonProperty(FIELD_LAST_ERROR)
            public abstract Builder lastError(@Nullable String lastError);

            abstract Data autoBuild();

            public Data build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }
}
