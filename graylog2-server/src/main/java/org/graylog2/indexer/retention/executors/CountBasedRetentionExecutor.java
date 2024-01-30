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
package org.graylog2.indexer.retention.executors;

import jakarta.inject.Inject;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class CountBasedRetentionExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CountBasedRetentionExecutor.class);

    private final Indices indices;
    private final ActivityWriter activityWriter;

    private final RetentionExecutor retentionExecutor;

    @Inject
    public CountBasedRetentionExecutor(Indices indices,
                                       ActivityWriter activityWriter,
                                       RetentionExecutor retentionExecutor) {
        this.indices = indices;
        this.activityWriter = activityWriter;
        this.retentionExecutor = retentionExecutor;
    }

    public void retain(IndexSet indexSet,
                       @Nullable Integer maxNumberOfIndices,
                       RetentionExecutor.RetentionAction action,
                       String actionName) {


        final Map<String, Set<String>> deflectorIndices = indexSet.getAllIndexAliases();
        final int indexCount = (int) deflectorIndices.keySet()
                .stream()
                .filter(indexName -> !indices.isReopened(indexName))
                .count();

        if (maxNumberOfIndices == null) {
            LOG.warn("No retention strategy configuration found, not running index retention!");
            return;
        }

        // Do we have more indices than the configured maximum?
        if (indexCount <= maxNumberOfIndices) {
            LOG.debug("Number of indices ({}) lower than limit ({}). Not performing any retention actions.",
                    indexCount, maxNumberOfIndices);
            return;
        }

        // We have more indices than the configured maximum! Remove as many as needed.
        final int removeCount = indexCount - maxNumberOfIndices;
        final String msg = "Number of indices (" + indexCount + ") higher than limit (" + maxNumberOfIndices + "). " +
                "Running retention for " + removeCount + " indices.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, CountBasedRetentionExecutor.class));

        retentionExecutor.runRetention(indexSet, removeCount, action, actionName);
    }
}
