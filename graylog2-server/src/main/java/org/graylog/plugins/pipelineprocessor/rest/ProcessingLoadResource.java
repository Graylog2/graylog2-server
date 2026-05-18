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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigService;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreterStateUpdater;
import org.graylog.plugins.pipelineprocessor.rest.ProcessingLoadService.ActiveCombination;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteMetricsResource;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "Pipelines/ProcessingLoad", description = "Pipeline rule Processing Load metric")
@Path("/system/pipelines/processing-load")
@Produces(MediaType.APPLICATION_JSON)
public class ProcessingLoadResource extends ProxiedResource {

    private final PipelineInterpreterStateUpdater stateUpdater;
    private final RuleMetricsConfigService ruleMetricsConfigService;
    private final ProcessingLoadService processingLoadService;
    private final NodeTimerSnapshotParser snapshotParser;
    private final Duration callTimeout;

    @Inject
    public ProcessingLoadResource(NodeService nodeService,
                                  RemoteInterfaceProvider remoteInterfaceProvider,
                                  @Context HttpHeaders httpHeaders,
                                  @Named("proxiedRequestsExecutorService") ExecutorService executorService,
                                  @Named("proxied_requests_default_call_timeout") com.github.joschi.jadconfig.util.Duration defaultCallTimeout,
                                  PipelineInterpreterStateUpdater stateUpdater,
                                  RuleMetricsConfigService ruleMetricsConfigService,
                                  ProcessingLoadService processingLoadService,
                                  NodeTimerSnapshotParser snapshotParser) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
        this.stateUpdater = stateUpdater;
        this.ruleMetricsConfigService = ruleMetricsConfigService;
        this.processingLoadService = processingLoadService;
        this.snapshotParser = snapshotParser;
        this.callTimeout = Duration.ofMillis(Math.min(defaultCallTimeout.toMilliseconds(), 2000));
    }

    @GET
    @Operation(summary = "Get cluster-aggregated pipeline rule Processing Load percentages",
               description = "Returns Processing Load % per pipeline-stage-rule, per pipeline, and per rule. " +
                       "Requires debug metrics enable.")
    public ProcessingLoadResponse processingLoad() {
        if (!ruleMetricsConfigService.get().metricsEnabled()) {
            return ProcessingLoadResponse.unavailable();
        }

        final PipelineInterpreter.State state = stateUpdater.getLatestState();
        final List<ActiveCombination> combinations = processingLoadService.activeCombinations(state);

        if (combinations.isEmpty()) {
            return ProcessingLoadResponse.unavailable();
        }

        final PermissionCache perms = new PermissionCache(
                id -> isPermitted(PipelineRestPermissions.PIPELINE_READ, id),
                id -> isPermitted(PipelineRestPermissions.PIPELINE_RULE_READ, id)
        );

        if (!hasAnyVisibleEntity(combinations, perms)) {
            throw new ForbiddenException("No read permission on any active pipeline or rule.");
        }

        final List<String> expectedTimerNames = processingLoadService.expectedTimerNames(combinations);
        final Map<String, NodeTimerSnapshot> perNodeSnapshots = fetchNodeSnapshots(expectedTimerNames);
        final ProcessingLoadResponse full = processingLoadService.compute(combinations, perNodeSnapshots);
        return processingLoadService.filterByPermissions(full, perms::canReadPipeline, perms::canReadRule);
    }

    private Map<String, NodeTimerSnapshot> fetchNodeSnapshots(List<String> metricNames) {
        final MetricsReadRequest request = MetricsReadRequest.create(metricNames);
        final Map<String, Optional<MetricsSummaryResponse>> perNodeResponses = stripCallResult(
                requestOnAllNodes(
                        RemoteMetricsResource.class,
                        r -> r.multipleMetrics(request),
                        callTimeout
                )
        );

        final Map<String, NodeTimerSnapshot> perNodeSnapshots = Maps.newHashMapWithExpectedSize(perNodeResponses.size());
        perNodeResponses.forEach((nodeId, response) ->
                perNodeSnapshots.put(nodeId, response.map(snapshotParser::parse).orElseGet(NodeTimerSnapshot::empty))
        );
        return perNodeSnapshots;
    }

    private static boolean hasAnyVisibleEntity(List<ActiveCombination> combinations, PermissionCache perms) {
        return combinations.stream()
                .anyMatch(c -> perms.canReadPipeline(c.pipelineId()) || perms.canReadRule(c.ruleId()));
    }

    /**
     * Caches per-id Shiro permission lookups so each id is asked once, not once per stage_rule entry.
     */
    private static final class PermissionCache {
        private final Predicate<String> pipelineLookup;
        private final Predicate<String> ruleLookup;
        private final Map<String, Boolean> pipelines = new HashMap<>();
        private final Map<String, Boolean> rules = new HashMap<>();

        PermissionCache(Predicate<String> pipelineLookup, Predicate<String> ruleLookup) {
            this.pipelineLookup = pipelineLookup;
            this.ruleLookup = ruleLookup;
        }

        boolean canReadPipeline(String pipelineId) {
            return pipelines.computeIfAbsent(pipelineId, pipelineLookup::test);
        }

        boolean canReadRule(String ruleId) {
            return rules.computeIfAbsent(ruleId, ruleLookup::test);
        }
    }
}
