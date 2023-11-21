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
package org.graylog2.datatiering.retention;

import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.datatiering.config.HotTierConfig;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

public class RetentionExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionExecutor.class);

    private final Indices indices;
    private final JobSchedulerClock clock;
    private final ActivityWriter activityWriter;

    @Inject
    public RetentionExecutor(Indices indices,
                             JobSchedulerClock clock,
                             ActivityWriter activityWriter) {
        this.indices = indices;
        this.clock = clock;
        this.activityWriter = activityWriter;
    }

    private static boolean hasCurrentWriteAlias(IndexSet indexSet, Map<String, Set<String>> deflectorIndices, String indexName) {
        return deflectorIndices.getOrDefault(indexName, Collections.emptySet()).contains(indexSet.getWriteIndexAlias());
    }

    public void retain(IndexSet indexSet, HotTierConfig config, Retention retention) {
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

        if (removeCount > 0) {
            final String msg = "Running retention for " + removeCount + " aged-out indices.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, RetentionExecutor.class));

            runRetention(indexSet, deflectorIndices, removeCount, retention);
        }
    }


    private void runRetention(IndexSet indexSet, Map<String, Set<String>> deflectorIndices, int removeCount, Retention retention) {
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

        final String msg = "Running data tier retention for indices <" + indexNamesAsString + ">";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, RetentionExecutor.class));

        retention.retain(orderedIndicesDescending);
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

    public interface Retention{
        void retain(List<String> indexNames);
    }

}
