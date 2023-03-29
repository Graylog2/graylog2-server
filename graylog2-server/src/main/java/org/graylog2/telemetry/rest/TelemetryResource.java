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
package org.graylog2.telemetry.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.flattener.JsonFlattener;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteSystemResource;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Telemetry", description = "Message inputs", tags = {CLOUD_VISIBLE})
@Path("/telemetry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TelemetryResource extends ProxiedResource {

    private final TelemetryService telemetryService;
    private ObjectMapper objectMapper;

    protected TelemetryResource(NodeService nodeService,
                                RemoteInterfaceProvider remoteInterfaceProvider,
                                @Context HttpHeaders httpHeaders,
                                @Named("proxiedRequestsExecutorService") ExecutorService executorService,
                                TelemetryService telemetryService,
                                ObjectMapper objectMapper
    ) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
    }

    @GET
    @ApiOperation(value = "Get telemetry information.")
    public String get() {
        TelemetryResponse telemetryResponse = telemetryService.createTelemetryResponse(getCurrentUser(), getSystemOverviewResponses());
        try {
            return JsonFlattener.flatten(objectMapper.writeValueAsString(telemetryResponse));
        } catch (JsonProcessingException e) {
            // TODO throw appropriate exception
            throw new RuntimeException(e);
        }
    }

    private Map<String, SystemOverviewResponse> getSystemOverviewResponses() {
        Map<String, SystemOverviewResponse> results = new HashMap<>();
        requestOnAllNodes(
                createRemoteInterfaceProvider(RemoteSystemResource.class),
                RemoteSystemResource::system)
                .forEach((s, r) -> results.put(s, toSystemOverviewResponse(r)));
        return results;
    }

    private SystemOverviewResponse toSystemOverviewResponse(CallResult<SystemOverviewResponse> callResult) {
        return Optional.ofNullable(callResult.response()).flatMap(NodeResponse::entity).orElse(null);
    }

}
