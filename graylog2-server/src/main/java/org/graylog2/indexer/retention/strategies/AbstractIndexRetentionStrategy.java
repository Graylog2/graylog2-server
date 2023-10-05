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
package org.graylog2.indexer.retention.strategies;

import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.periodical.IndexRetentionThread;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.f;

public abstract class AbstractIndexRetentionStrategy implements RetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIndexRetentionStrategy.class);

    private final Indices indices;
    private final ActivityWriter activityWriter;
    private JobSchedulerClock clock;

    protected AbstractIndexRetentionStrategy(Indices indices,
                                          ActivityWriter activityWriter,
                                          JobSchedulerClock clock) {
        this.indices = requireNonNull(indices);
        this.activityWriter = requireNonNull(activityWriter);
        this.clock = clock;
    }

    protected abstract Optional<Integer> getMaxNumberOfIndices(IndexSet indexSet);
    protected abstract void retain(List<String> indexNames, IndexSet indexSet);

    @Override
    public void retain(IndexSet indexSet) {
        if (indexSet.getConfig().rotationStrategy() instanceof TimeBasedSizeOptimizingStrategyConfig timeBasedConfig) {
            retainTimeBased(indexSet, timeBasedConfig);
        } else {
            retainCountBased(indexSet);
        }
    }

    private void retainTimeBased(IndexSet indexSet, TimeBasedSizeOptimizingStrategyConfig timeBasedConfig) {
        final Map<String, Set<String>> deflectorIndices = indexSet.getAllIndexAliases();

        // Account for DST and time zones in determining age
        final DateTime now = clock.nowUTC();
        final long cutoffSoft = now.minus(timeBasedConfig.indexLifetimeMin()).getMillis();
        final long cutoffHard = now.minus(timeBasedConfig.indexLifetimeMax()).getMillis();
        final int removeCount = (int)deflectorIndices.keySet()
                .stream()
                .filter(indexName -> !indices.isReopened(indexName))
                .filter(indexName -> !hasCurrentWriteAlias(indexSet, deflectorIndices, indexName))
                .filter(indexName -> exceedsAgeLimit(indexName, cutoffSoft, cutoffHard))
                .count();

        if (removeCount > 0) {
            final String msg = "Running retention for " + removeCount + " aged-out indices.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, IndexRetentionThread.class));

            runRetention(indexSet, deflectorIndices, removeCount);
        } else {
            var debug = deflectorIndices.keySet().stream()
                    .collect(Collectors.toMap(k -> k, k -> Map.of(
                            "isReopened", indices.isReopened(k),
                            "hasCurrentWriteAlias", hasCurrentWriteAlias(indexSet, deflectorIndices, k),
                            "exceedsAgeLimit", exceedsAgeLimit(k, cutoffSoft, cutoffHard),
                            "closingDate", indices.indexClosingDate(k),
                            "creationDate", indices.indexCreationDate(k)
                    )));
            LOG.debug("Nothing to retain for indexSet <{}>: (min {}, max {}) details: <{}>",
                    indexSet.getIndexPrefix(), timeBasedConfig.indexLifetimeMin(), timeBasedConfig.indexLifetimeMax(), debug);
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

    private void retainCountBased(IndexSet indexSet) {
        final Map<String, Set<String>> deflectorIndices = indexSet.getAllIndexAliases();
        final int indexCount = (int)deflectorIndices.keySet()
                .stream()
                .filter(indexName -> !indices.isReopened(indexName))
                .count();

        final Optional<Integer> maxIndices = getMaxNumberOfIndices(indexSet);

        if (!maxIndices.isPresent()) {
            LOG.warn("No retention strategy configuration found, not running index retention!");
            return;
        }

        // Do we have more indices than the configured maximum?
        if (indexCount <= maxIndices.get()) {
            LOG.debug("Number of indices ({}) lower than limit ({}). Not performing any retention actions.",
                    indexCount, maxIndices.get());
            return;
        }

        // We have more indices than the configured maximum! Remove as many as needed.
        final int removeCount = indexCount - maxIndices.get();
        final String msg = "Number of indices (" + indexCount + ") higher than limit (" + maxIndices.get() + "). " +
                "Running retention for " + removeCount + " indices.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, IndexRetentionThread.class));

        runRetention(indexSet, deflectorIndices, removeCount);
    }

    private void runRetention(IndexSet indexSet, Map<String, Set<String>> deflectorIndices, int removeCount) {
        final Set<String> orderedIndices = Arrays.stream(indexSet.getManagedIndices())
            .filter(indexName -> !indices.isReopened(indexName))
            .filter(indexName -> !hasCurrentWriteAlias(indexSet, deflectorIndices, indexName))
            .sorted((indexName1, indexName2) -> indexSet.extractIndexNumber(indexName2).orElse(0).compareTo(indexSet.extractIndexNumber(indexName1).orElse(0)))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        LinkedList<String> orderedIndicesDescending = new LinkedList<>();

        orderedIndices
                .stream()
                .skip(orderedIndices.size() - removeCount)
                // reverse order to archive oldest index first
                .collect(Collectors.toCollection(LinkedList::new)).descendingIterator().
                forEachRemaining(orderedIndicesDescending::add);

        String indexNamesAsString = String.join(", ", orderedIndicesDescending);

        final String strategyName = this.getClass().getCanonicalName();
        final String msg = "Running retention strategy [" + strategyName + "] for indices <" + indexNamesAsString + ">";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, IndexRetentionThread.class));

        retain(orderedIndicesDescending, indexSet);
    }

    private static boolean hasCurrentWriteAlias(IndexSet indexSet, Map<String, Set<String>> deflectorIndices, String indexName) {
        return deflectorIndices.getOrDefault(indexName, Collections.emptySet()).contains(indexSet.getWriteIndexAlias());
    }
}
