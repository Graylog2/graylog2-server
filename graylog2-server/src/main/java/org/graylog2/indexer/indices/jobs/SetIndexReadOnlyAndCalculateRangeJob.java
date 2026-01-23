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
import org.graylog.scheduler.system.SystemJob;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobContext;
import org.graylog.scheduler.system.SystemJobInfo;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog.scheduler.system.SystemJobResult;
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePoller;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

import static org.graylog2.cluster.lock.ClusterLockResources.indexModification;

public class SetIndexReadOnlyAndCalculateRangeJob implements SystemJob<SetIndexReadOnlyAndCalculateRangeJob.Config> {
    public static final String TYPE_NAME = "set-index-read-only-and-calculate-range";

    private static final Logger LOG = LoggerFactory.getLogger(SetIndexReadOnlyAndCalculateRangeJob.class);
    private final IndexSetRegistry indexSetRegistry;
    private final Indices indices;
    private final IndexFieldTypesService indexFieldTypesService;
    private final IndexFieldTypePoller indexFieldTypePoller;
    private final ActivityWriter activityWriter;
    private final SystemJobManager systemJobManager;
    private final RefreshingLockService.Factory lockServiceFactory;

    public interface Factory extends SystemJob.Factory<SetIndexReadOnlyAndCalculateRangeJob> {
        @Override
        SetIndexReadOnlyAndCalculateRangeJob create();
    }

    @Inject
    public SetIndexReadOnlyAndCalculateRangeJob(IndexSetRegistry indexSetRegistry,
                                                Indices indices,
                                                IndexFieldTypesService indexFieldTypesService,
                                                IndexFieldTypePoller indexFieldTypePoller,
                                                ActivityWriter activityWriter,
                                                SystemJobManager systemJobManager,
                                                RefreshingLockService.Factory lockServiceFactory) {
        this.indexSetRegistry = indexSetRegistry;
        this.indices = indices;
        this.indexFieldTypesService = indexFieldTypesService;
        this.indexFieldTypePoller = indexFieldTypePoller;
        this.activityWriter = activityWriter;
        this.systemJobManager = systemJobManager;
        this.lockServiceFactory = lockServiceFactory;
    }

    @Override
    public SystemJobResult execute(Config config, SystemJobContext ctx) {
        try (var lockService = lockServiceFactory.create()) {
            lockService.acquireAndKeepLock(indexModification(config.indexName()), UUID.randomUUID().toString());

            return doExecute(config.indexName());
        } catch (AlreadyLockedException e) {
            LOG.debug("Recalculation for index <{}> already running.", config.indexName());
            // We don't retry here because setting an index to read-only doesn't make much sense to do multiple times.
            return SystemJobResult.success();
        } catch (Exception e) {
            LOG.error("Error while setting index <{}> to read-only and calculating index range", config.indexName(), e);
            // TODO: Handle retries once the job scheduler supports retry tracking.
            return SystemJobResult.withError();
        }
    }

    public SystemJobResult doExecute(String indexName) {
        if (!indices.exists(indexName)) {
            LOG.debug("Not running job for deleted index <{}>", indexName);
            return SystemJobResult.success();
        }
        if (indices.isClosed(indexName)) {
            LOG.debug("Not running job for closed index <{}>", indexName);
            return SystemJobResult.success();
        }

        final Optional<IndexSet> indexSet = indexSetRegistry.getForIndex(indexName);
        if (indexSet.isEmpty()) {
            LOG.error("Couldn't find index set for index <{}>", indexName);
            return SystemJobResult.withError();
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
            systemJobManager.submit(OptimizeIndexJob.forIndex(
                    indexName,
                    indexSet.get().getConfig().indexOptimizationMaxNumSegments()
            ));
        }

        systemJobManager.submit(CreateNewSingleIndexRangeJob.forIndex(indexName));

        // Update field type information again to make sure we got the latest state
        indexSetRegistry.getForIndex(indexName)
                .flatMap(is -> indexFieldTypePoller.pollIndex(indexName, is.getConfig().id()))
                .ifPresent(indexFieldTypesService::upsert);

        return SystemJobResult.success();
    }

    public static Config forIndex(String indexName) {
        return Config.Builder.create().indexName(indexName).build();
    }

    @AutoValue
    @JsonDeserialize(builder = SetIndexReadOnlyAndCalculateRangeJob.Config.Builder.class)
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements SystemJobConfig {
        @JsonProperty("index_name")
        public abstract String indexName();

        @Override
        public SystemJobInfo toInfo() {
            return SystemJobInfo.builder()
                    .type(type())
                    .description("Sets index to read-only and updates index range information.")
                    .statusInfo("Setting index to read-only and calculating ranges for index " + indexName() + ".")
                    .isCancelable(false)
                    .reportsProgress(false)
                    .build();
        }

        @AutoValue.Builder
        public static abstract class Builder implements SystemJobConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_SetIndexReadOnlyAndCalculateRangeJob_Config.Builder().type(TYPE_NAME);
            }

            @JsonProperty("index_name")
            public abstract Builder indexName(String indexName);

            public abstract Config build();
        }
    }
}
