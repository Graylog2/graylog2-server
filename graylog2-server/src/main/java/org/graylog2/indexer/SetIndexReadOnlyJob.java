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
package org.graylog2.indexer;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SetIndexReadOnlyJob extends SystemJob {
    private static final Logger log = LoggerFactory.getLogger(SetIndexReadOnlyJob.class);

    public interface Factory {
        SetIndexReadOnlyJob create(String index);

    }

    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;
    private final OptimizeIndexJob.Factory optimizeIndexJobFactory;
    private final SystemJobManager systemJobManager;
    private final String index;
    private final ActivityWriter activityWriter;

    @AssistedInject
    public SetIndexReadOnlyJob(Indices indices,
                               IndexSetRegistry indexSetRegistry,
                               SystemJobManager systemJobManager,
                               OptimizeIndexJob.Factory optimizeIndexJobFactory,
                               ActivityWriter activityWriter,
                               @Assisted String index) {
        this.indices = indices;
        this.indexSetRegistry = indexSetRegistry;
        this.optimizeIndexJobFactory = optimizeIndexJobFactory;
        this.systemJobManager = systemJobManager;
        this.index = index;
        this.activityWriter = activityWriter;
    }

    @Override
    public void execute() {
        if (!indices.exists(index)) {
            log.debug("Not running job for deleted index <{}>", index);
            return;
        }
        if (indices.isClosed(index)) {
            log.debug("Not running job for closed index <{}>", index);
            return;
        }

        final Optional<IndexSet> indexSet = indexSetRegistry.getForIndex(index);

        if (!indexSet.isPresent()) {
            log.error("Couldn't find index set for index <{}>", index);
            return;
        }

        log.info("Flushing old index <{}>.", index);
        indices.flush(index);

        log.info("Setting old index <{}> to read-only.", index);
        indices.setReadOnly(index);

        activityWriter.write(new Activity("Flushed and set <" + index + "> to read-only.", SetIndexReadOnlyJob.class));

        if (!indexSet.get().getConfig().indexOptimizationDisabled()) {
            try {
                systemJobManager.submit(optimizeIndexJobFactory.create(index, indexSet.get().getConfig().indexOptimizationMaxNumSegments()));
            } catch (SystemJobConcurrencyException e) {
                // The concurrency limit is very high. This should never happen.
                log.error("Cannot optimize index <" + index + ">.", e);
            }
        }
    }

    @Override
    public void requestCancel() {
        // not possible, ignore
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public int maxConcurrency() {
        return 1000;
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
        return "Sets an index to read only for performance and optionally triggers an optimization.";
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public String getInfo() {
        return "Setting index " + index + " to read-only.";
    }
}
