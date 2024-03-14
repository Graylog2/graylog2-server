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

import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePoller;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SetIndexReadOnlyAndCalculateRangeJob extends SystemJob {
    private static final Logger LOG = LoggerFactory.getLogger(SetIndexReadOnlyAndCalculateRangeJob.class);
    private final OptimizeIndexJob.Factory optimizeIndexJobFactory;
    private final CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory;
    private final IndexSetRegistry indexSetRegistry;
    private final Indices indices;
    private final IndexFieldTypesService indexFieldTypesService;
    private final IndexFieldTypePoller indexFieldTypePoller;
    private final ActivityWriter activityWriter;
    private final SystemJobManager systemJobManager;
    private final String indexName;

    @Inject
    public SetIndexReadOnlyAndCalculateRangeJob(OptimizeIndexJob.Factory optimizeIndexJobFactory,
                                                CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory,
                                                IndexSetRegistry indexSetRegistry,
                                                Indices indices,
                                                IndexFieldTypesService indexFieldTypesService,
                                                IndexFieldTypePoller indexFieldTypePoller,
                                                ActivityWriter activityWriter,
                                                SystemJobManager systemJobManager,
                                                @Assisted String indexName) {
        this.optimizeIndexJobFactory = optimizeIndexJobFactory;
        this.createNewSingleIndexRangeJobFactory = createNewSingleIndexRangeJobFactory;
        this.indexSetRegistry = indexSetRegistry;
        this.indices = indices;
        this.indexFieldTypesService = indexFieldTypesService;
        this.indexFieldTypePoller = indexFieldTypePoller;
        this.activityWriter = activityWriter;
        this.systemJobManager = systemJobManager;
        this.indexName = indexName;
    }

    @Override
    public void execute() {
        if (!indices.exists(indexName)) {
            LOG.debug("Not running job for deleted index <{}>", indexName);
            return;
        }
        if (indices.isClosed(indexName)) {
            LOG.debug("Not running job for closed index <{}>", indexName);
            return;
        }
        setReadonly();
        final SystemJob createNewSingleIndexRangeJob = createNewSingleIndexRangeJobFactory.create(indexSetRegistry.getAll(), indexName);
        createNewSingleIndexRangeJob.execute();

        // Update field type information again to make sure we got the latest state
        indexSetRegistry.getForIndex(indexName)
                .ifPresent(indexSet -> {
                    indexFieldTypePoller.pollIndex(indexName, indexSet.getConfig().id())
                            .ifPresent(indexFieldTypesService::upsert);
                });
    }

    public String getIndex() {
        return indexName;
    }

    public void setReadonly() {
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
            try {
                systemJobManager.submit(optimizeIndexJobFactory.create(indexName, indexSet.get().getConfig().indexOptimizationMaxNumSegments()));
            } catch (SystemJobConcurrencyException e) {
                // The concurrency limit is very high. This should never happen.
                LOG.error("Cannot optimize index <" + indexName + ">.", e);
            }
        }
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
        return "Makes index " + indexName + " read only and calculates and adds its index range afterwards.";
    }

    @Override
    public String getInfo() {
        return "Make index <%s> read only and calculate ranges".formatted(indexName);
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }

    public interface Factory {
        SetIndexReadOnlyAndCalculateRangeJob create(String indexName);
    }
}
