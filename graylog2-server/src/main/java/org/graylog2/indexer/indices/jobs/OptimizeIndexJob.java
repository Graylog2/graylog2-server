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
import com.google.inject.assistedinject.AssistedInject;
import jakarta.inject.Named;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.system.SystemJob;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobContext;
import org.graylog.scheduler.system.SystemJobInfo;
import org.graylog.scheduler.system.SystemJobResult;
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.graylog2.cluster.lock.ClusterLockResources.concurrentIndexOptimization;
import static org.graylog2.cluster.lock.ClusterLockResources.indexModification;

public class OptimizeIndexJob implements SystemJob<OptimizeIndexJob.Config> {
    public static final String TYPE_NAME = "optimize-index";

    private static final Logger LOG = LoggerFactory.getLogger(OptimizeIndexJob.class);
    private final Indices indices;
    private final ActivityWriter activityWriter;
    private final RefreshingLockService.Factory lockServiceFactory;
    private final Duration indexOptimizationTimeout;
    private final int indexOptimizationJobs;

    public interface Factory extends SystemJob.Factory<OptimizeIndexJob> {
        @Override
        OptimizeIndexJob create();
    }

    @AssistedInject
    public OptimizeIndexJob(Indices indices,
                            ActivityWriter activityWriter,
                            RefreshingLockService.Factory lockServiceFactory,
                            @Named("elasticsearch_index_optimization_timeout") Duration indexOptimizationTimeout,
                            @Named("elasticsearch_index_optimization_jobs") int indexOptimizationJobs) {
        this.indices = indices;
        this.activityWriter = activityWriter;
        this.lockServiceFactory = lockServiceFactory;
        this.indexOptimizationTimeout = indexOptimizationTimeout;
        this.indexOptimizationJobs = indexOptimizationJobs;
    }

    @Override
    public SystemJobResult execute(Config config, SystemJobContext ctx) throws JobExecutionException {
        // First, acquire a global lock to limit the number of concurrent index optimization jobs
        try (var concurrencyLockService = lockServiceFactory.create()) {
            concurrencyLockService.acquireAndKeepLock(concurrentIndexOptimization(), indexOptimizationJobs);

            // Now, acquire a lock for the specific index to optimize
            try (var lockService = lockServiceFactory.create()) {
                lockService.acquireAndKeepLock(indexModification(config.indexName()), UUID.randomUUID().toString());

                return doExecute(config.indexName(), config.maxNumSegments());
            } catch (AlreadyLockedException e) {
                LOG.debug("Index optimization for index <{}> already running, scheduling retry.", config.indexName());
                return SystemJobResult.withRetry(java.time.Duration.ofSeconds(5), Integer.MAX_VALUE);
            }
        } catch (AlreadyLockedException e) {
            LOG.debug("Maximum of {} concurrent index optimization jobs reached, scheduling retry.", indexOptimizationJobs);
            return SystemJobResult.withRetry(java.time.Duration.ofSeconds(5), Integer.MAX_VALUE);
        } catch (Exception e) {
            LOG.error("Error optimizing index <{}>", config.indexName(), e);
            return SystemJobResult.withError();
        }
    }

    public SystemJobResult doExecute(String index, int maxNumSegments) {
        if (!indices.exists(index)) {
            LOG.debug("Not running job for deleted index <{}>", index);
            return SystemJobResult.success();
        }
        if (indices.isClosed(index)) {
            LOG.debug("Not running job for closed index <{}>", index);
            return SystemJobResult.success();
        }

        String msg = "Optimizing index <" + index + ">.";
        activityWriter.write(new Activity(msg, OptimizeIndexJob.class));
        LOG.info(msg);

        indices.optimizeIndex(index, maxNumSegments, indexOptimizationTimeout);

        return SystemJobResult.success();
    }

    public static Config forIndex(String indexName, int maxNumSegments) {
        return Config.Builder.create().indexName(indexName).maxNumSegments(maxNumSegments).build();
    }

    @AutoValue
    @JsonDeserialize(builder = OptimizeIndexJob.Config.Builder.class)
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements SystemJobConfig {
        @JsonProperty("index_name")
        public abstract String indexName();

        @JsonProperty("max_num_segments")
        public abstract int maxNumSegments();

        @Override
        public SystemJobInfo toInfo() {
            return SystemJobInfo.builder()
                    .type(type())
                    .description("Optimizes an index for read performance.")
                    .statusInfo("Optimizing index <" + indexName() + ">.")
                    .isCancelable(false)
                    .reportsProgress(false)
                    .build();
        }

        @AutoValue.Builder
        public static abstract class Builder implements SystemJobConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_OptimizeIndexJob_Config.Builder().type(TYPE_NAME);
            }

            @JsonProperty("index_name")
            public abstract Builder indexName(String indexName);

            @JsonProperty("max_num_segments")
            public abstract Builder maxNumSegments(int maxNumSegments);

            public abstract Config build();
        }
    }
}
