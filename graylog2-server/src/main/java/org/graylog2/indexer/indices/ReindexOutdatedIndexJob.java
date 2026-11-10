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
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.system.SystemJob;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobContext;
import org.graylog.scheduler.system.SystemJobInfo;
import org.graylog.scheduler.system.SystemJobResult;
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.graylog2.cluster.lock.ClusterLockResources.indexModification;

/**
 * One-shot system job that reindexes a single outdated index into a format compatible with the next major OpenSearch
 * version. The actual work is delegated to {@link OutdatedIndexService#reindex(String, boolean)} and runs once on a
 * system-scheduler worker thread, so the triggering REST request returns immediately instead of blocking for the whole
 * reindex.
 */
public class ReindexOutdatedIndexJob implements SystemJob<ReindexOutdatedIndexJob.Config> {
    private static final Logger LOG = LoggerFactory.getLogger(ReindexOutdatedIndexJob.class);

    public static final String TYPE_NAME = "reindex-outdated-index-v1";

    private final OutdatedIndexService outdatedIndexService;
    private final RefreshingLockService.Factory lockServiceFactory;

    public interface Factory extends SystemJob.Factory<ReindexOutdatedIndexJob> {
        @Override
        ReindexOutdatedIndexJob create();
    }

    @AssistedInject
    public ReindexOutdatedIndexJob(OutdatedIndexService outdatedIndexService,
                                   RefreshingLockService.Factory lockServiceFactory) {
        this.outdatedIndexService = outdatedIndexService;
        this.lockServiceFactory = lockServiceFactory;
    }

    @Override
    public SystemJobResult execute(Config config, SystemJobContext ctx) throws JobExecutionException {
        final String index = config.index();
        // Cluster-wide, per-index lock held for the whole run. Reindexing is destructive (the source index is deleted
        // and recreated), so two reindexes — or a reindex and another index modification — must never run against the
        // same index at once.
        try (var lockService = lockServiceFactory.create()) {
            lockService.acquireAndKeepLock(indexModification(index), UUID.randomUUID().toString());

            LOG.info("Reindexing outdated index {} (withReplicas={}), triggered by {}",
                    index, config.withReplicas(), config.triggeredBy());
            outdatedIndexService.reindex(index, config.withReplicas());
            LOG.info("Finished reindexing outdated index {}", index);
            return SystemJobResult.success();
        } catch (AlreadyLockedException e) {
            // Another modification is already in progress for this index. We must NOT retry: a retry would eventually
            // run a second destructive reindex once the lock frees. Skip this duplicate instead.
            LOG.info("Skipping reindex of index {}: another index modification is already in progress.", index);
            return SystemJobResult.success();
        } catch (Exception e) {
            LOG.error("Reindexing outdated index {} failed", index, e);
            return SystemJobResult.withError();
        }
    }

    public static Config forIndex(String index, boolean withReplicas, String triggeredBy) {
        return Config.builder()
                .index(index)
                .withReplicas(withReplicas)
                .triggeredBy(triggeredBy)
                .build();
    }

    @AutoValue
    @JsonTypeName(ReindexOutdatedIndexJob.TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public abstract static class Config implements SystemJobConfig {

        public static final String FIELD_INDEX = "index";
        public static final String FIELD_WITH_REPLICAS = "with_replicas";
        public static final String FIELD_TRIGGERED_BY = "triggered_by";

        @JsonProperty(FIELD_INDEX)
        public abstract String index();

        @JsonProperty(FIELD_WITH_REPLICAS)
        public abstract boolean withReplicas();

        @JsonProperty(FIELD_TRIGGERED_BY)
        public abstract String triggeredBy();

        @Override
        public SystemJobInfo toInfo() {
            return SystemJobInfo.builder()
                    .type(type())
                    .description("Reindexes an outdated index to the current major OpenSearch version.")
                    .statusInfo("Reindexing outdated index <" + index() + ">.")
                    .isCancelable(false)
                    .reportsProgress(false)
                    .build();
        }

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder implements SystemJobConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_ReindexOutdatedIndexJob_Config.Builder().type(TYPE_NAME);
            }

            @JsonProperty(FIELD_INDEX)
            public abstract Builder index(String index);

            @JsonProperty(FIELD_WITH_REPLICAS)
            public abstract Builder withReplicas(boolean withReplicas);

            @JsonProperty(FIELD_TRIGGERED_BY)
            public abstract Builder triggeredBy(String triggeredBy);

            abstract Config autoBuild();

            public Config build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }
}
