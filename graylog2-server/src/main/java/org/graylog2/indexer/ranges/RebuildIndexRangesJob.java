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

import com.google.common.base.Stopwatch;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RebuildIndexRangesJob extends SystemJob {
    public interface Factory {
        RebuildIndexRangesJob create(Set<IndexSet> indexSets);
    }

    private static final Logger LOG = LoggerFactory.getLogger(RebuildIndexRangesJob.class);
    private static final int MAX_CONCURRENCY = 1;

    private volatile boolean cancelRequested = false;
    private volatile int indicesToCalculate = 0;
    private final AtomicInteger indicesCalculated = new AtomicInteger(0);

    protected final Set<IndexSet> indexSets;
    private final ActivityWriter activityWriter;
    protected final IndexRangeService indexRangeService;

    @AssistedInject
    public RebuildIndexRangesJob(@Assisted Set<IndexSet> indexSets,
                                 ActivityWriter activityWriter,
                                 IndexRangeService indexRangeService) {
        this.indexSets = indexSets;
        this.activityWriter = activityWriter;
        this.indexRangeService = indexRangeService;
    }

    @Override
    public void requestCancel() {
        this.cancelRequested = true;
    }

    @Override
    public int getProgress() {
        if (indicesToCalculate <= 0) {
            return 0;
        }

        // lolwtfbbqcasting
        return (int) Math.floor((indicesCalculated.floatValue() / (float) indicesToCalculate) * 100);
    }

    @Override
    public String getDescription() {
        return "Rebuilds index range information.";
    }

    @Override
    public void execute() {
        info("Recalculating index ranges.");

        // for each index set we know about
        final ListMultimap<IndexSet, String> indexSets = MultimapBuilder.hashKeys().arrayListValues().build();
        for (IndexSet indexSet : this.indexSets) {
            final String[] managedIndicesNames = indexSet.getManagedIndices();
            for (String name : managedIndicesNames) {
                indexSets.put(indexSet, name);
            }
        }

        if (indexSets.size() == 0) {
            info("No indices, nothing to calculate.");
            return;
        }
        indicesToCalculate = indexSets.values().size();

        Stopwatch sw = Stopwatch.createStarted();
        for (IndexSet indexSet : indexSets.keySet()) {
            LOG.info("Recalculating index ranges for index set {} ({}): {} indices affected.",
                    indexSet.getConfig().title(),
                    indexSet.getIndexWildcard(),
                    indexSets.get(indexSet).size());
            for (String index : indexSets.get(indexSet)) {
                try {
                    if (index.equals(indexSet.getActiveWriteIndex())) {
                        LOG.debug("{} is current write target, do not calculate index range for it", index);
                        final IndexRange emptyRange = indexRangeService.createUnknownRange(index);
                        try {
                            final IndexRange indexRange = indexRangeService.get(index);
                            if (indexRange.begin().getMillis() != 0 || indexRange.end().getMillis() != 0) {
                                LOG.info("Invalid date ranges for write index {}, resetting it.", index);
                                indexRangeService.save(emptyRange);
                            }
                        } catch (NotFoundException e) {
                            LOG.info("No index range found for write index {}, recreating it.", index);
                            indexRangeService.save(emptyRange);
                        }

                        indicesCalculated.incrementAndGet();
                        continue;
                    }
                } catch (TooManyAliasesException e) {
                    LOG.error("Multiple write alias targets found, this is a bug.");
                    indicesCalculated.incrementAndGet();
                    continue;
                }
                if (cancelRequested) {
                    info("Stop requested. Not calculating next index range, not updating ranges.");
                    sw.stop();
                    return;
                }

                try {
                    final IndexRange indexRange = indexRangeService.calculateRange(index);
                    indexRangeService.save(indexRange);
                    LOG.info("Created ranges for index {}: {}", index, indexRange);
                } catch (Exception e) {
                    LOG.info("Could not calculate range of index [" + index + "]. Skipping.", e);
                } finally {
                    indicesCalculated.incrementAndGet();
                }
            }
        }

        info("Done calculating index ranges for " + indicesToCalculate + " indices. Took " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms.");
    }

    protected void info(String what) {
        LOG.info(what);
        activityWriter.write(new Activity(what, RebuildIndexRangesJob.class));
    }

    @Override
    public boolean providesProgress() {
        return true;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    public int maxConcurrency() {
        return MAX_CONCURRENCY;
    }

    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }
}
