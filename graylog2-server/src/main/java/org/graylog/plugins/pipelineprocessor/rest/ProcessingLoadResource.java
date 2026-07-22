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
import org.graylog.plugins.pipelineprocessor.rest.ProcessingLoadService.ActiveCombination;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteMetricsResource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "Pipelines/ProcessingLoad", description = "Pipeline rule Processing Load metric")
@Path("/system/pipelines/processing-load")
@Produces(MediaType.APPLICATION_JSON)
public class ProcessingLoadResource extends ProxiedResource {

    private final ProcessingLoadBuilder processingLoadBuilder;
    private final ProcessingLoadService processingLoadService;

    @Inject
    public ProcessingLoadResource(NodeService nodeService,
                                  RemoteInterfaceProvider remoteInterfaceProvider,
                                  @Context HttpHeaders httpHeaders,
                                  @Named("proxiedRequestsExecutorService") ExecutorService executorService,
                                  ProcessingLoadBuilder processingLoadBuilder,
                                  ProcessingLoadService processingLoadService) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
        this.processingLoadBuilder = processingLoadBuilder;
        this.processingLoadService = processingLoadService;
    }

    @GET
    @NoAuditEvent("Read-only endpoint, no audit event needed")
    @Operation(summary = "Get cluster-aggregated pipeline rule Processing Load percentages",
               description = "Returns Processing Load % per pipeline-stage-rule, per pipeline, and per rule. " +
                       "Requires debug metrics enable.")
    public ProcessingLoadResponse processingLoad() {
        final List<ActiveCombination> combinations = processingLoadBuilder.activeCombinations();
        if (combinations.isEmpty()) {
            return ProcessingLoadResponse.unavailable();
        }
        if (combinations.stream().noneMatch(c -> canReadPipeline(c.pipelineId()) || canReadRule(c.ruleId()))) {
            throw new ForbiddenException("No read permission on any active pipeline or rule.");
        }

        final ProcessingLoadResponse full = processingLoadBuilder.buildUnfiltered(combinations, this::fetchPerNodeMetrics);
        return processingLoadService.filterByPermissions(full, this::canReadPipeline, this::canReadRule);
    }

    private Map<String, MetricsSummaryResponse> fetchPerNodeMetrics(List<String> timerNames) {
        final MetricsReadRequest request = MetricsReadRequest.create(timerNames);
        return stripCallResult(requestOnAllNodes(RemoteMetricsResource.class, r -> r.multipleMetrics(request)))
                .entrySet().stream()
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    private boolean canReadPipeline(String pipelineId) {
        return isPermitted(PipelineRestPermissions.PIPELINE_READ, pipelineId);
    }

    private boolean canReadRule(String ruleId) {
        return isPermitted(PipelineRestPermissions.PIPELINE_RULE_READ, ruleId);
    }
}
