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
package org.graylog2.indexer.ranges;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.inject.Inject;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.system.SystemJob;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobContext;
import org.graylog.scheduler.system.SystemJobInfo;
import org.graylog.scheduler.system.SystemJobResult;
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.graylog2.indexer.indices.Indices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

import static org.graylog2.cluster.lock.ClusterLockResources.indexRangeRecalculation;

public class CreateNewSingleIndexRangeJob implements SystemJob<CreateNewSingleIndexRangeJob.Config> {
    public static final String TYPE_NAME = "create-new-single-index-range";
    private static final Logger LOG = LoggerFactory.getLogger(CreateNewSingleIndexRangeJob.class);

    private final Indices indices;
    private final IndexRangeService indexRangeService;
    private final RefreshingLockService.Factory lockServiceFactory;

    public interface Factory extends SystemJob.Factory<CreateNewSingleIndexRangeJob> {
        @Override
        CreateNewSingleIndexRangeJob create();
    }

    @Inject
    public CreateNewSingleIndexRangeJob(Indices indices,
                                        IndexRangeService indexRangeService,
                                        RefreshingLockService.Factory lockServiceFactory) {
        this.indices = indices;
        this.indexRangeService = indexRangeService;
        this.lockServiceFactory = lockServiceFactory;
    }

    @Override
    public SystemJobResult execute(Config config, SystemJobContext ctx) throws JobExecutionException {
        try {
            return doExecute(config.indexName());
        } catch (Exception e) {
            LOG.error("Error while creating new single index range for index <{}>", config.indexName(), e);
            return SystemJobResult.withError();
        }
    }

    public SystemJobResult doExecute(String indexName) throws JobExecutionException {
        if (!indices.exists(indexName)) {
            LOG.debug("Not running job for deleted index <{}>", indexName);
            return SystemJobResult.success();
        }
        if (indices.isClosed(indexName)) {
            LOG.debug("Not running job for closed index <{}>", indexName);
            return SystemJobResult.success();
        }

        try (var lockService = lockServiceFactory.create()) {
            lockService.acquireAndKeepLock(indexRangeRecalculation(indexName), UUID.randomUUID().toString());

            if (!indexRangeService.calculateRangeAndSave(indexName)) {
                return SystemJobResult.withError();
            }
            return SystemJobResult.success();
        } catch (AlreadyLockedException e) {
            LOG.debug("Recalculation for index <{}> already running, scheduling retry.", indexName);
            return SystemJobResult.withRetry(Duration.ofSeconds(5));
        }
    }

    public static Config forIndex(String indexName) {
        return Config.Builder.create().indexName(indexName).build();
    }

    @AutoValue
    @JsonDeserialize(builder = Config.Builder.class)
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements SystemJobConfig {
        @JsonProperty("index_name")
        public abstract String indexName();

        @Override
        public SystemJobInfo toInfo() {
            return SystemJobInfo.builder()
                    .type(type())
                    .description("Creates new single index range information.")
                    .statusInfo("Calculating ranges for index " + indexName() + ".")
                    .isCancelable(false)
                    .reportsProgress(false)
                    .build();
        }

        @AutoValue.Builder
        public static abstract class Builder implements SystemJobConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_CreateNewSingleIndexRangeJob_Config.Builder().type(TYPE_NAME);
            }

            @JsonProperty("index_name")
            public abstract Builder indexName(String indexName);

            public abstract Config build();
        }
    }
}
