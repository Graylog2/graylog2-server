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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.system.SystemJob;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobContext;
import org.graylog.scheduler.system.SystemJobInfo;
import org.graylog.scheduler.system.SystemJobResult;
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.ClusterLockResources;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.basic.BasicIndexSet;
import org.graylog2.indexer.indexset.basic.ExtendedBasicIndexSets;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RebuildIndexRangesJob implements SystemJob<RebuildIndexRangesJob.Config> {
    public static final String TYPE_NAME = "rebuild-all-index-ranges";

    private static final Logger LOG = LoggerFactory.getLogger(RebuildIndexRangesJob.class);
    private static final int MAX_CONCURRENCY = 1;


    private final ActivityWriter activityWriter;
    protected final IndexRangeService indexRangeService;
    private final IndexSetRegistry indexSetRegistry;
    private final IndexSetService indexSetService;
    private final Set<ExtendedBasicIndexSets> extendedBasicIndexSets;
    private final RefreshingLockService.Factory lockServiceFactory;

    public interface Factory extends SystemJob.Factory<RebuildIndexRangesJob> {
        @Override
        RebuildIndexRangesJob create();
    }

    @AssistedInject
    public RebuildIndexRangesJob(ActivityWriter activityWriter,
                                 IndexRangeService indexRangeService,
                                 IndexSetRegistry indexSetRegistry,
                                 IndexSetService indexSetService,
                                 Set<ExtendedBasicIndexSets> extendedBasicIndexSets,
                                 RefreshingLockService.Factory lockServiceFactory) {
        this.activityWriter = activityWriter;
        this.indexRangeService = indexRangeService;
        this.indexSetRegistry = indexSetRegistry;
        this.indexSetService = indexSetService;
        this.extendedBasicIndexSets = extendedBasicIndexSets;
        this.lockServiceFactory = lockServiceFactory;
    }

    @Override
    public SystemJobResult execute(Config config, SystemJobContext ctx) throws JobExecutionException {
        try {
            return doExecute(config, ctx);
        } catch (Exception e) {
            LOG.error("Error while rebuilding index ranges.", e);
            return SystemJobResult.withError();
        }
    }

    public SystemJobResult doExecute(Config config, SystemJobContext ctx) throws JobExecutionException {
        info("Recalculating index ranges.");

        // for each index set we know about
        final ListMultimap<BasicIndexSet, IndexInfo> indexSets = loadIndexSets(config);

        if (indexSets.isEmpty()) {
            info("Nothing to calculate.");
            return SystemJobResult.success();
        }

        final int indicesToCalculate = indexSets.values().size();
        final AtomicInteger indicesCalculated = new AtomicInteger(0);

        final Stopwatch sw = Stopwatch.createStarted();
        for (final BasicIndexSet indexSet : indexSets.keySet()) {
            LOG.info("Recalculating index ranges for index set {} ({}): {} indices affected.",
                    indexSet.title(),
                    indexSet.getIndexWildcard(),
                    indexSets.get(indexSet).size());

            for (final var indexInfo : indexSets.get(indexSet)) {
                final var index = indexInfo.name();

                try (var lockService = lockServiceFactory.create()) {
                    lockService.acquireAndKeepLock(ClusterLockResources.indexRangeRecalculation(index), UUID.randomUUID().toString());

                    try {
                        if (indexInfo.isActiveWriteIndex()) {
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

                            ctx.updateProgress(indicesToCalculate, indicesCalculated.incrementAndGet());
                            continue;
                        }
                    } catch (TooManyAliasesException e) {
                        LOG.error("Multiple write alias targets found, this is a bug.");
                        ctx.updateProgress(indicesToCalculate, indicesCalculated.incrementAndGet());
                        continue;
                    }
                    if (ctx.isCancelled()) {
                        info("Stop requested. Not calculating next index range, not updating ranges.");
                        return SystemJobResult.success();
                    }

                    indexRangeService.calculateRangeAndSave(index);
                    ctx.updateProgress(indicesToCalculate, indicesCalculated.incrementAndGet());
                } catch (AlreadyLockedException e) {
                    LOG.debug("Recalculation for index <{}> already running, scheduling retry.", index);
                    return SystemJobResult.withRetry(Duration.ofSeconds(5), Integer.MAX_VALUE);
                }
            }
        }

        info("Done calculating index ranges for " + indicesToCalculate + " indices. Took " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms.");

        return SystemJobResult.success();
    }

    private record IndexInfo(String name, boolean isActiveWriteIndex) {}

    private ListMultimap<BasicIndexSet, IndexInfo> loadIndexSets(Config config) {
        final ListMultimap<BasicIndexSet, IndexInfo> indexSetIndices = MultimapBuilder.hashKeys().arrayListValues().build();

        // Shortcut
        if (config.indexSetIds().isEmpty() && config.extendedBasicIndexSetClasses().isEmpty() && !config.rebuildAll()) {
            return indexSetIndices;
        }

        final Set<? extends BasicIndexSet> basicIndexSets;
        if (config.rebuildAll()) {
            basicIndexSets = indexSetRegistry.getAllBasicIndexSets();
        } else {
            basicIndexSets = Stream.concat(
                    indexSetRegistry.getFromIndexConfig(indexSetService.findByIds(config.indexSetIds())).stream(),
                    extendedBasicIndexSets.stream()
                            .filter(ebis -> config.extendedBasicIndexSetClasses().contains(ebis.getClass().getCanonicalName()))
                            .flatMap(ebis -> ebis.indexSets().stream())
            ).collect(Collectors.toSet());
        }

        for (final var basicIndexSet : basicIndexSets) {
            for (final var indexName : basicIndexSet.getManagedIndices()) {
                // Ugh...
                if (basicIndexSet instanceof IndexSet indexSet) {
                    indexSetIndices.put(basicIndexSet, new IndexInfo(indexName, indexName.equals(indexSet.getActiveWriteIndex())));
                } else {
                    // BasicIndexSet has no concept of active write index.
                    indexSetIndices.put(basicIndexSet, new IndexInfo(indexName, false));
                }
            }
        }

        return indexSetIndices;
    }

    public static Config forIndexSets(Set<IndexSet> indexSets) {
        final var indexSetIds = indexSets.stream()
                .map(IndexSet::getConfig)
                .map(IndexSetConfig::id)
                .collect(Collectors.toSet());
        return Config.Builder.create().indexSetIds(indexSetIds).build();
    }

    public static Config forExtendedBasicIndexSetClasses(Set<? extends ExtendedBasicIndexSets> extendedBasicIndexSets) {
        // There is no other way to address ExtendedBasicIndexSets than via their class names.
        final var classNames = extendedBasicIndexSets.stream()
                .map(i -> i.getClass().getCanonicalName())
                .collect(Collectors.toSet());
        return Config.Builder.create().extendedBasicIndexSetClasses(classNames).build();
    }

    public static Config rebuildAll() {
        return Config.Builder.create().rebuildAll(true).build();
    }

    @AutoValue
    @JsonDeserialize(builder = RebuildIndexRangesJob.Config.Builder.class)
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements SystemJobConfig {
        @JsonProperty("index_set_ids")
        public abstract Set<String> indexSetIds();

        @JsonProperty("extended_basic_index_set_classes")
        public abstract Set<String> extendedBasicIndexSetClasses();

        @JsonProperty("rebuild_all")
        public abstract boolean rebuildAll();

        @Override
        public SystemJobInfo toInfo() {
            var statusInfo = "Rebuilding index ranges for <" + indexSetIds().size() + "> index sets.";
            if (rebuildAll()) {
                statusInfo = "Rebuilding index ranges for all indices.";
            }
            return SystemJobInfo.builder()
                    .type(type())
                    .description("Rebuild index ranges for index sets.")
                    .statusInfo(statusInfo)
                    .isCancelable(true)
                    .reportsProgress(true)
                    .build();
        }

        @AutoValue.Builder
        public static abstract class Builder implements SystemJobConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_RebuildIndexRangesJob_Config.Builder()
                        .type(TYPE_NAME)
                        .indexSetIds(Set.of())
                        .extendedBasicIndexSetClasses(Set.of())
                        .rebuildAll(false);
            }

            @JsonProperty("index_set_ids")
            public abstract Builder indexSetIds(Set<String> indexSetIds);

            @JsonProperty("extended_basic_index_set_classes")
            public abstract Builder extendedBasicIndexSetClasses(Set<String> extendedBasicIndexSetClasses);

            @JsonProperty("rebuild_all")
            public abstract Builder rebuildAll(boolean rebuildAll);

            public abstract Config build();
        }
    }

    protected void info(String what) {
        LOG.info(what);
        activityWriter.write(new Activity(what, RebuildIndexRangesJob.class));
    }
}
