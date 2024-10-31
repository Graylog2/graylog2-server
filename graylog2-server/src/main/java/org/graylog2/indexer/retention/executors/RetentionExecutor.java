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
import org.graylog2.periodical.IndexRetentionThread;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RetentionExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionExecutor.class);
    private final ActivityWriter activityWriter;
    private final Indices indices;

    @Inject
    public RetentionExecutor(ActivityWriter activityWriter, Indices indices) {
        this.activityWriter = activityWriter;
        this.indices = indices;
    }

    private static boolean hasCurrentWriteAlias(IndexSet indexSet, Map<String, Set<String>> deflectorIndices, String indexName) {
        return deflectorIndices.getOrDefault(indexName, Collections.emptySet()).contains(indexSet.getWriteIndexAlias());
    }

    public void runRetention(IndexSet indexSet,
                             int removeCount,
                             RetentionAction action,
                             String actionName) {
        Map<String, Set<String>> deflectorIndices = indexSet.getAllIndexAliases();

        final Set<String> orderedIndices = Arrays.stream(indexSet.getManagedIndices())
                .filter(indexName -> !indices.isReopened(indexName))
                .filter(indexName -> !hasCurrentWriteAlias(indexSet, deflectorIndices, indexName))
                .sorted(indexSet.indexComparator())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        LinkedList<String> orderedIndicesDescending = new LinkedList<>();

        orderedIndices
                .stream()
                .skip(orderedIndices.size() - removeCount)
                // reverse order to archive oldest index first
                .collect(Collectors.toCollection(LinkedList::new)).descendingIterator().
                forEachRemaining(orderedIndicesDescending::add);

        String indexNamesAsString = String.join(", ", orderedIndicesDescending);

        final String msg = "Running retention action [" + actionName + "] for indices <" + indexNamesAsString + ">";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, IndexRetentionThread.class));

        action.retain(orderedIndicesDescending, indexSet);
    }

    public interface RetentionAction {

        void retain(List<String> indexNames, IndexSet indexSet);
    }
}
