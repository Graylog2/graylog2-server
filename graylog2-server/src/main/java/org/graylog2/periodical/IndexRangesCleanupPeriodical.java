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
package org.graylog2.periodical;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.Ints;
import com.google.inject.name.Named;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import static java.util.Objects.requireNonNull;

/**
 * A {@link Periodical} to clean up stale index ranges (e. g. because the index has been deleted externally)
 *
 * @since 1.3.0
 */
public class IndexRangesCleanupPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRangesCleanupPeriodical.class);

    private final Cluster cluster;
    private final IndexSetRegistry indexSetRegistry;
    private final IndexRangeService indexRangeService;
    private final EventBus eventBus;
    private final int periodSeconds;

    @Inject
    public IndexRangesCleanupPeriodical(final Cluster cluster,
                                        final IndexSetRegistry indexSetRegistry,
                                        final IndexRangeService indexRangeService,
                                        final EventBus eventBus,
                                        @Named("index_ranges_cleanup_interval") final Duration indexRangesCleanupInterval) {
        this.cluster = requireNonNull(cluster);
        this.indexSetRegistry = requireNonNull(indexSetRegistry);
        this.indexRangeService = requireNonNull(indexRangeService);
        this.eventBus = requireNonNull(eventBus);
        this.periodSeconds = Ints.saturatedCast(indexRangesCleanupInterval.toSeconds());

    }

    @Override
    public void doRun() {
        if (!cluster.isConnected() || !cluster.isHealthy()) {
            LOG.info("Skipping index range cleanup because the Elasticsearch cluster is unreachable or unhealthy");
            return;
        }

        final Set<String> indexNames = ImmutableSet.copyOf(indexSetRegistry.getManagedIndices());
        final SortedSet<IndexRange> indexRanges = indexRangeService.findAll();

        final Set<String> removedIndices = new HashSet<>();
        for (IndexRange indexRange : indexRanges) {
            if (!indexNames.contains(indexRange.indexName())) {
                removedIndices.add(indexRange.indexName());
            }
        }

        if (!removedIndices.isEmpty()) {
            LOG.info("Removing index range information for unavailable indices: {}", removedIndices);
            eventBus.post(IndicesDeletedEvent.create(removedIndices));
        }
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 15;
    }

    @Override
    public int getPeriodSeconds() {
        return periodSeconds;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
