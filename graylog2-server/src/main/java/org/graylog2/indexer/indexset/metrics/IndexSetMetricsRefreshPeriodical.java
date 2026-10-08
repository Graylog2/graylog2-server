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
package org.graylog2.indexer.indexset.metrics;

import com.google.common.primitives.Ints;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.metrics.entity.EntityMetricsService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;

import static org.graylog2.metrics.entity.EntityMetricsModule.ENTITY_TYPE_INDEX_SETS;
import static org.graylog2.metrics.entity.cache.MetricsCacheConfiguration.METRICS_CACHE_TTL_SHORT;

/**
 * Leader-only {@link Periodical} that keeps the cached metrics of all index sets fresh by recomputing them once per
 * short cache TTL.
 */
public class IndexSetMetricsRefreshPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetMetricsRefreshPeriodical.class);

    private final EntityMetricsService metricsService;
    private final IndexSetService indexSetService;
    private final int periodSeconds;

    @Inject
    public IndexSetMetricsRefreshPeriodical(@Named(ENTITY_TYPE_INDEX_SETS) EntityMetricsService metricsService,
                                            IndexSetService indexSetService,
                                            @Named(METRICS_CACHE_TTL_SHORT) Duration shortTtl) {
        this.metricsService = metricsService;
        this.indexSetService = indexSetService;
        this.periodSeconds = Ints.saturatedCast(shortTtl.toSeconds());
    }

    @Override
    public void doRun() {
        metricsService.refresh(indexSetService.findAll().stream().map(IndexSetConfig::id).toList());
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
    public boolean leaderOnly() {
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
        return 60;
    }

    @Override
    public int getPeriodSeconds() {
        return periodSeconds;
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
