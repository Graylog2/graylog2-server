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
package org.graylog.plugins.pipelineprocessor.rest;

import com.google.common.collect.Maps;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigService;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreterStateUpdater;
import org.graylog.plugins.pipelineprocessor.rest.ProcessingLoadService.ActiveCombination;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;

import java.util.List;
import java.util.Map;

/**
 * Builds a {@link ProcessingLoadResponse} from cluster-wide debug timer data, shared by the
 * {@code /system/pipelines/processing-load} endpoint and the support-bundle snapshot so both stay
 * in sync. Callers pass a {@link NodeMetricsFetcher}, keeping this class agnostic of how the
 * per-node {@code multipleMetrics} fan-out is transported.
 */
@Singleton
public class ProcessingLoadBuilder {

    private final PipelineInterpreterStateUpdater stateUpdater;
    private final RuleMetricsConfigService ruleMetricsConfigService;
    private final ProcessingLoadService processingLoadService;
    private final NodeTimerSnapshotParser snapshotParser;

    @Inject
    public ProcessingLoadBuilder(PipelineInterpreterStateUpdater stateUpdater,
                                     RuleMetricsConfigService ruleMetricsConfigService,
                                     ProcessingLoadService processingLoadService,
                                     NodeTimerSnapshotParser snapshotParser) {
        this.stateUpdater = stateUpdater;
        this.ruleMetricsConfigService = ruleMetricsConfigService;
        this.processingLoadService = processingLoadService;
        this.snapshotParser = snapshotParser;
    }

    public boolean metricsEnabled() {
        return ruleMetricsConfigService.get().metricsEnabled();
    }

    /**
     * The active pipeline-stage-rule combinations, or empty if debug metrics are off. Callers can
     * permission-check these before the cluster fan-out, then pass the same list to
     * {@link #buildUnfiltered(List, NodeMetricsFetcher)} to avoid resolving them twice.
     */
    List<ActiveCombination> activeCombinations() {
        if (!metricsEnabled()) {
            return List.of();
        }
        return processingLoadService.activeCombinations(stateUpdater.getLatestState());
    }

    /**
     * Builds the unfiltered response, resolving the active combinations itself. Returns
     * {@code unavailable} when metrics are off, no combinations are active, or no node reported data.
     */
    public ProcessingLoadResponse buildUnfiltered(NodeMetricsFetcher fetcher) {
        return buildUnfiltered(activeCombinations(), fetcher);
    }

    /**
     * Builds the unfiltered response from already-resolved combinations, so a caller that needed
     * them (e.g. for a permission check) doesn't recompute them.
     */
    ProcessingLoadResponse buildUnfiltered(List<ActiveCombination> combinations, NodeMetricsFetcher fetcher) {
        if (combinations.isEmpty()) {
            return ProcessingLoadResponse.unavailable();
        }

        final List<String> timerNames = processingLoadService.expectedTimerNames(combinations);
        final Map<String, MetricsSummaryResponse> perNodeResponses = fetcher.fetch(timerNames);
        final Map<String, NodeTimerSnapshot> perNodeSnapshots = Maps.newHashMapWithExpectedSize(perNodeResponses.size());
        perNodeResponses.forEach((nodeId, response) -> perNodeSnapshots.put(nodeId, snapshotParser.parse(response)));

        return processingLoadService.compute(combinations, perNodeSnapshots);
    }

    /**
     * Fans a {@code multipleMetrics} call out to every node, returning responses keyed by node id.
     * The implementation supplies the transport (REST proxy vs. support-bundle helper).
     * A node missing from the map simply has no data.
     */
    @FunctionalInterface
    public interface NodeMetricsFetcher {
        Map<String, MetricsSummaryResponse> fetch(List<String> timerNames);
    }
}
