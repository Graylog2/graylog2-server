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
package org.graylog2.outputs.filter;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog2.plugin.Message;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class PipelineRuleOutputFilterState extends PipelineInterpreter.State {
    public interface Factory {
        PipelineRuleOutputFilterState newState(@Assisted("pipelines") ImmutableMap<String, Pipeline> pipelines,
                                               @Assisted("destinations") ImmutableSet<String> destinations,
                                               @Assisted("activeStreams") ImmutableSet<String> activeStreams);
    }

    private final ImmutableSet<String> destinations;
    private final ImmutableSet<String> activeStreams;

    @Inject
    public PipelineRuleOutputFilterState(@Assisted("pipelines") ImmutableMap<String, Pipeline> pipelines,
                                         @Assisted("destinations") ImmutableSet<String> destinations,
                                         @Assisted("activeStreams") ImmutableSet<String> activeStreams,
                                         MetricRegistry metricRegistry,
                                         @Named("processbuffer_processors") int processorCount,
                                         @Named("cached_stageiterators") boolean cachedIterators) {
        super(
                pipelines,
                ImmutableSetMultimap.of(),
                RuleMetricsConfigDto.createDefault(),
                metricRegistry,
                processorCount,
                cachedIterators
        );
        this.destinations = destinations;
        this.activeStreams = activeStreams;
    }

    public boolean isEmpty() {
        return getCurrentPipelines().isEmpty();
    }

    public ImmutableSet<String> getDestinations() {
        return destinations;
    }

    public ImmutableSet<String> getActiveStreams() {
        return activeStreams;
    }

    public Set<Pipeline> getPipelinesForMessage(Message msg) {
        // The current pipelines are keyed by stream
        return msg.getStreamIds().stream()
                .map(id -> getCurrentPipelines().get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    protected String getStageCacheMetricName() {
        return name(PipelineRuleOutputFilter.class, STAGE_CACHE_METRIC_SUFFIX);
    }
}
