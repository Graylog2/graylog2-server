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

import com.github.joschi.jadconfig.util.Duration;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

public class OptimizeIndexJob extends SystemJob {
    public interface Factory {
        OptimizeIndexJob create(String index, int maxNumSegments);
    }

    private static final Logger LOG = LoggerFactory.getLogger(OptimizeIndexJob.class);

    private final Indices indices;
    private final ActivityWriter activityWriter;
    private final Duration indexOptimizationTimeout;
    private final int indexOptimizationJobs;
    private final String index;
    private final int maxNumSegments;

    @AssistedInject
    public OptimizeIndexJob(Indices indices,
                            ActivityWriter activityWriter,
                            @Named("elasticsearch_index_optimization_timeout") Duration indexOptimizationTimeout,
                            @Named("elasticsearch_index_optimization_jobs") int indexOptimizationJobs,
                            @Assisted String index,
                            @Assisted int maxNumSegments) {
        this.indices = indices;
        this.activityWriter = activityWriter;
        this.indexOptimizationTimeout = indexOptimizationTimeout;
        this.indexOptimizationJobs = indexOptimizationJobs;
        this.index = index;
        this.maxNumSegments = maxNumSegments;
    }

    @Override
    public void execute() {
        if (indices.isClosed(index)) {
            LOG.debug("Not running job for closed index <{}>", index);
            return;
        }

        String msg = "Optimizing index <" + index + ">.";
        activityWriter.write(new Activity(msg, OptimizeIndexJob.class));
        LOG.info(msg);

        indices.optimizeIndex(index, maxNumSegments, indexOptimizationTimeout);
    }

    @Override
    public void requestCancel() {
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public int maxConcurrency() {
        return indexOptimizationJobs;
    }

    @Override
    public boolean providesProgress() {
        return false;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Optimizes an index for read performance.";
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public String getInfo() {
        return "Optimizing index " + index + ".";
    }
}
