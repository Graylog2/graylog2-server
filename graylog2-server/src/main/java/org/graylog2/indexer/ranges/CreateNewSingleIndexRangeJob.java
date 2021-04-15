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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class CreateNewSingleIndexRangeJob extends RebuildIndexRangesJob {
    private static final Logger LOG = LoggerFactory.getLogger(CreateNewSingleIndexRangeJob.class);
    private final String indexName;
    private final Indices indices;

    public interface Factory {
        CreateNewSingleIndexRangeJob create(Set<IndexSet> indexSets, String indexName);
    }

    @AssistedInject
    public CreateNewSingleIndexRangeJob(@Assisted Set<IndexSet> indexSets,
                                        @Assisted String indexName,
                                        ActivityWriter activityWriter,
                                        Indices indices,
                                        IndexRangeService indexRangeService) {
        super(indexSets, activityWriter, indexRangeService);
        this.indexName = checkNotNull(indexName);
        this.indices = indices;
    }

    @Override
    public String getDescription() {
        return "Creates new single index range information.";
    }

    @Override
    public String getInfo() {
        return "Calculating ranges for index " + indexName + ".";
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
        LOG.info("Calculating ranges for index {}.", indexName);
        try {
            final IndexRange indexRange = indexRangeService.calculateRange(indexName);
            indexRangeService.save(indexRange);
            LOG.info("Created ranges for index {}.", indexName);
        } catch (Exception e) {
            LOG.error("Exception during index range calculation for index " + indexName, e);
        }
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
    public int maxConcurrency() {
        // Actually we need some sort of queuing for SystemJobs.
        return Integer.MAX_VALUE;
    }
}
