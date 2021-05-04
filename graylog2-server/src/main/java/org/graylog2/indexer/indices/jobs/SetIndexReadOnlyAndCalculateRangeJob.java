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
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.SetIndexReadOnlyJob;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePoller;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class SetIndexReadOnlyAndCalculateRangeJob extends SystemJob {
    private static final Logger LOG = LoggerFactory.getLogger(SetIndexReadOnlyAndCalculateRangeJob.class);

    public interface Factory {
        SetIndexReadOnlyAndCalculateRangeJob create(String indexName);
    }

    private final SetIndexReadOnlyJob.Factory setIndexReadOnlyJobFactory;
    private final CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory;
    private final IndexSetRegistry indexSetRegistry;
    private final Indices indices;
    private final IndexFieldTypesService indexFieldTypesService;
    private final IndexFieldTypePoller indexFieldTypePoller;
    private final String indexName;

    @Inject
    public SetIndexReadOnlyAndCalculateRangeJob(SetIndexReadOnlyJob.Factory setIndexReadOnlyJobFactory,
                                                CreateNewSingleIndexRangeJob.Factory createNewSingleIndexRangeJobFactory,
                                                IndexSetRegistry indexSetRegistry,
                                                Indices indices,
                                                IndexFieldTypesService indexFieldTypesService,
                                                IndexFieldTypePoller indexFieldTypePoller,
                                                @Assisted String indexName) {
        this.setIndexReadOnlyJobFactory = setIndexReadOnlyJobFactory;
        this.createNewSingleIndexRangeJobFactory = createNewSingleIndexRangeJobFactory;
        this.indexSetRegistry = indexSetRegistry;
        this.indices = indices;
        this.indexFieldTypesService = indexFieldTypesService;
        this.indexFieldTypePoller = indexFieldTypePoller;
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
        final SystemJob setIndexReadOnlyJob = setIndexReadOnlyJobFactory.create(indexName);
        setIndexReadOnlyJob.execute();
        final SystemJob createNewSingleIndexRangeJob = createNewSingleIndexRangeJobFactory.create(indexSetRegistry.getAll(), indexName);
        createNewSingleIndexRangeJob.execute();

        // Update field type information again to make sure we got the latest state
        indexSetRegistry.getForIndex(indexName)
                .ifPresent(indexSet -> {
                    indexFieldTypePoller.pollIndex(indexName, indexSet.getConfig().id())
                            .ifPresent(indexFieldTypesService::upsert);
                });
    }

    @Override
    public void requestCancel() {}

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
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }
}
