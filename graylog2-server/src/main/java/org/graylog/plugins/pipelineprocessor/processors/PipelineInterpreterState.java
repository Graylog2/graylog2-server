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
package org.graylog.plugins.pipelineprocessor.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog2.metrics.CacheStatsSet;
import org.graylog2.shared.metrics.MetricUtils;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Named;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.codahale.metrics.MetricRegistry.name;

public class PipelineInterpreterState {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineInterpreterState.class);
    public static final String STAGE_CACHE_METRIC_SUFFIX = "stage-cache";

    private final ImmutableMap<String, Pipeline> currentPipelines;
    private final ImmutableSetMultimap<String, Pipeline> streamPipelineConnections;
    private final LoadingCache<Set<Pipeline>, StageIterator.Configuration> cache;
    private final boolean cachedIterators;
    private final RuleMetricsConfigDto ruleMetricsConfig;

    @AssistedInject
    public PipelineInterpreterState(@Assisted ImmutableMap<String, Pipeline> currentPipelines,
                                    @Assisted ImmutableSetMultimap<String, Pipeline> streamPipelineConnections,
                                    @Assisted RuleMetricsConfigDto ruleMetricsConfig,
                                    MetricRegistry metricRegistry,
                                    @Named("processbuffer_processors") int processorCount,
                                    @Named("cached_stageiterators") boolean cachedIterators) {
        this.currentPipelines = currentPipelines;
        this.streamPipelineConnections = streamPipelineConnections;
        this.cachedIterators = cachedIterators;
        this.ruleMetricsConfig = ruleMetricsConfig;

        cache = CacheBuilder.newBuilder()
                .concurrencyLevel(processorCount)
                .recordStats()
                .build(new CacheLoader<Set<Pipeline>, StageIterator.Configuration>() {
                    @Override
                    public StageIterator.Configuration load(@Nonnull Set<Pipeline> pipelines) {
                        return new StageIterator.Configuration(pipelines);
                    }
                });

        // we have to remove the metrics, because otherwise we leak references to the cache (and the register call with throw)
        metricRegistry.removeMatching((name, metric) -> name.startsWith(getStageCacheMetricName()));
        MetricUtils.safelyRegisterAll(metricRegistry, new CacheStatsSet(getStageCacheMetricName(), cache));
    }

    public String getStageCacheMetricName() {
        return name(PipelineInterpreter.class, STAGE_CACHE_METRIC_SUFFIX);
    }

    public ImmutableMap<String, Pipeline> getCurrentPipelines() {
        return currentPipelines;
    }

    public ImmutableSetMultimap<String, Pipeline> getStreamPipelineConnections() {
        return streamPipelineConnections;
    }

    public boolean enableRuleMetrics() {
        return ruleMetricsConfig.metricsEnabled();
    }

    public StageIterator getStageIterator(Set<Pipeline> pipelines) {
        try {
            if (cachedIterators) {
                return new StageIterator(cache.get(pipelines));
            } else {
                return new StageIterator(pipelines);
            }
        } catch (ExecutionException e) {
            LOG.error("Unable to get StageIterator from cache, this should not happen.", ExceptionUtils.getRootCause(e));
            return new StageIterator(pipelines);
        }
    }


    public interface Factory {
        PipelineInterpreterState newState(ImmutableMap<String, Pipeline> currentPipelines,
                                          ImmutableSetMultimap<String, Pipeline> streamPipelineConnections,
                                          RuleMetricsConfigDto ruleMetricsConfig);
    }
}
