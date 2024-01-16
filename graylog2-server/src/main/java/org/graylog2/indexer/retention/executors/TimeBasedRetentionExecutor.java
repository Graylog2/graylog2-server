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

import com.google.common.base.Joiner;
import jakarta.inject.Inject;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

public class TimeBasedRetentionExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(TimeBasedRetentionExecutor.class);

    private final Indices indices;
    private final JobSchedulerClock clock;
    private final ActivityWriter activityWriter;
    private final RetentionExecutor retentionExecutor;

    @Inject
    public TimeBasedRetentionExecutor(Indices indices,
                                      JobSchedulerClock clock,
                                      ActivityWriter activityWriter,
                                      RetentionExecutor retentionExecutor) {
        this.indices = indices;
        this.clock = clock;
        this.activityWriter = activityWriter;
        this.retentionExecutor = retentionExecutor;
    }

    private static boolean hasCurrentWriteAlias(IndexSet indexSet, Map<String, Set<String>> deflectorIndices, String indexName) {
        return deflectorIndices.getOrDefault(indexName, Collections.emptySet()).contains(indexSet.getWriteIndexAlias());
    }

    public void retain(IndexSet indexSet, IndexLifetimeConfig config, RetentionExecutor.RetentionAction action, String actionName) {
        final Map<String, Set<String>> deflectorIndices = indexSet.getAllIndexAliases();

        // Account for DST and time zones in determining age
        final DateTime now = clock.nowUTC();
        final long cutoffSoft = now.minus(config.indexLifetimeMin()).getMillis();
        final long cutoffHard = now.minus(config.indexLifetimeMax()).getMillis();
        final int removeCount = (int) deflectorIndices.keySet()
                .stream()
                .filter(indexName -> !indices.isReopened(indexName))
                .filter(indexName -> !hasCurrentWriteAlias(indexSet, deflectorIndices, indexName))
                .filter(indexName -> exceedsAgeLimit(indexName, cutoffSoft, cutoffHard))
                .count();

        if (LOG.isDebugEnabled()) {
            var debug = deflectorIndices.keySet().stream()
                    .collect(Collectors.toMap(k -> k, k -> Map.of(
                            "isReopened", indices.isReopened(k),
                            "hasCurrentWriteAlias", hasCurrentWriteAlias(indexSet, deflectorIndices, k),
                            "exceedsAgeLimit", exceedsAgeLimit(k, cutoffSoft, cutoffHard),
                            "closingDate", indices.indexClosingDate(k),
                            "creationDate", indices.indexCreationDate(k)
                    )));
            Joiner.MapJoiner mapJoiner = Joiner.on("\n").withKeyValueSeparator("=");
            LOG.debug("Debug info retain for indexSet <{}>: (min {}, max {}) removeCount: {} details: <{}>",
                    indexSet.getIndexPrefix(), config.indexLifetimeMin(), config.indexLifetimeMax(),
                    removeCount, mapJoiner.join(debug));
        }

        if (removeCount > 0) {
            final String msg = "Running retention for " + removeCount + " aged-out indices.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, TimeBasedRetentionExecutor.class));

            retentionExecutor.runRetention(indexSet, removeCount, action, actionName);
        }
    }


    private boolean exceedsAgeLimit(String indexName, long cutoffSoft, long cutoffHard) {
        Optional<DateTime> closingDate = indices.indexClosingDate(indexName);
        if (closingDate.isPresent()) {
            return closingDate.get().isBefore(cutoffSoft + 1);
        }

        Optional<DateTime> creationDate = indices.indexCreationDate(indexName);
        if (creationDate.isPresent()) {
            return creationDate.get().isBefore(cutoffHard + 1);
        }

        LOG.warn(f("Unable to determine creation or closing dates for Index %s - forcing retention", indexName));
        return true;
    }
}
