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
package org.graylog.collectors.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.collectors.CollectorInputService;
import org.graylog.collectors.CollectorLogsDestinationService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.input.CollectorIngestGrpcInput;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog.collectors.opamp.OpAmpCaService;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.rest.resources.RestResource;

import java.net.URI;

@Tag(name = "Collectors/Config", description = "Managed collector configuration")
@Path("/collectors/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class CollectorsConfigResource extends RestResource {

    static final int DEFAULT_HTTP_PORT = 14401;
    static final int DEFAULT_GRPC_PORT = 14402;

    private final ClusterConfigService clusterConfigService;
    private final CollectorInputService collectorInputService;
    private final CollectorLogsDestinationService collectorLogsDestinationService;
    private final URI httpExternalUri;
    private final FleetService fleetService;
    private final FleetTransactionLogService fleetTransactionLogService;
    private final OpAmpCaService opAmpCaService;

    @Inject
    public CollectorsConfigResource(ClusterConfigService clusterConfigService,
                                    CollectorInputService collectorInputService,
                                    CollectorLogsDestinationService collectorLogsDestinationService,
                                    HttpConfiguration httpConfiguration,
                                    FleetService fleetService,
                                    FleetTransactionLogService fleetTransactionLogService,
                                    OpAmpCaService opAmpCaService) {
        this.clusterConfigService = clusterConfigService;
        this.collectorInputService = collectorInputService;
        this.collectorLogsDestinationService = collectorLogsDestinationService;
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
        this.fleetService = fleetService;
        this.fleetTransactionLogService = fleetTransactionLogService;
        this.opAmpCaService = opAmpCaService;
    }

    @GET
    @Operation(summary = "Get collectors configuration")
    public CollectorsConfig get(@Context ContainerRequestContext requestContext) {
        final var existing = clusterConfigService.get(CollectorsConfig.class);
        if (existing != null) {
            return existing;
        }

        final var hostname = RestTools.buildExternalUri(requestContext.getHeaders(), httpExternalUri).getHost();
        return new CollectorsConfig(
                null, null, null,
                new IngestEndpointConfig(true, hostname, DEFAULT_HTTP_PORT, null),
                new IngestEndpointConfig(false, hostname, DEFAULT_GRPC_PORT, null)
        );
    }

    @NoAuditEvent("TODO")
    @PUT
    @Operation(summary = "Update collectors configuration")
    public CollectorsConfig put(@Valid @NotNull @RequestBody(required = true, useParameterTypeSchema = true) CollectorsConfigRequest request) {
        opAmpCaService.ensureInitialized();
        collectorLogsDestinationService.ensureExists();

        final var existing = clusterConfigService.get(CollectorsConfig.class);
        final String creatorUserId = SecurityUtils.getSubject().getPrincipal().toString();

        final String httpInputId = collectorInputService.reconcile(
                request.http(),
                existing != null ? existing.http() : null,
                CollectorIngestHttpInput.class.getCanonicalName(),
                CollectorIngestHttpInput.NAME,
                creatorUserId);

        final String grpcInputId = collectorInputService.reconcile(
                request.grpc(),
                existing != null ? existing.grpc() : null,
                CollectorIngestGrpcInput.class.getCanonicalName(),
                CollectorIngestGrpcInput.NAME,
                creatorUserId);

        final var config = new CollectorsConfig(
                opAmpCaService.getOpAmpCaId(),
                opAmpCaService.getTokenSigningCertId(),
                opAmpCaService.getOtlpServerCertId(),
                new IngestEndpointConfig(request.http().enabled(), request.http().hostname(),
                        request.http().port(), httpInputId),
                new IngestEndpointConfig(request.grpc().enabled(), request.grpc().hostname(),
                        request.grpc().port(), grpcInputId)
        );

        clusterConfigService.write(config);

        final var fleetIds = fleetService.getAllFleetIds();
        if (!fleetIds.isEmpty()) {
            fleetTransactionLogService.appendFleetMarker(fleetIds, MarkerType.INGEST_CONFIG_CHANGED);
        }

        return config;
    }
}
