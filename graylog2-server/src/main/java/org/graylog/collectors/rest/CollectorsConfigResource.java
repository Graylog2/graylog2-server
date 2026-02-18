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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog.collectors.input.CollectorIngestGrpcInput;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.opamp.OpAmpCaService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

@Tag(name = "Collectors/Config", description = "Managed collector configuration")
@Path("/collectors/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class CollectorsConfigResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorsConfigResource.class);

    static final int DEFAULT_HTTP_PORT = 14401;
    static final int DEFAULT_GRPC_PORT = 14402;

    private final ClusterConfigService clusterConfigService;
    private final URI httpExternalUri;
    private final InputService inputService;
    private final OpAmpCaService opAmpCaService;

    @Inject
    public CollectorsConfigResource(ClusterConfigService clusterConfigService,
                                    HttpConfiguration httpConfiguration,
                                    InputService inputService,
                                    OpAmpCaService opAmpCaService) {
        this.clusterConfigService = clusterConfigService;
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
        this.inputService = inputService;
        this.opAmpCaService = opAmpCaService;
    }

    @GET
    @Operation(summary = "Get collectors configuration")
    public CollectorsConfig get(@Context jakarta.ws.rs.container.ContainerRequestContext requestContext) {
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
    public CollectorsConfig put(@Valid @NotNull CollectorsConfigRequest request) {
        // 1. Initialize CA hierarchy (creates certs if needed, caches in memory)
        opAmpCaService.ensureInitialized();

        final var existing = clusterConfigService.get(CollectorsConfig.class);

        // 2. Reconcile HTTP input
        final String httpInputId = reconcileInput(
                request.http(),
                existing != null ? existing.http() : null,
                CollectorIngestHttpInput.class.getCanonicalName(),
                CollectorIngestHttpInput.NAME);

        // 3. Reconcile gRPC input
        final String grpcInputId = reconcileInput(
                request.grpc(),
                existing != null ? existing.grpc() : null,
                CollectorIngestGrpcInput.class.getCanonicalName(),
                CollectorIngestGrpcInput.NAME);

        // 4. Build and persist full config
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
        return config;
    }

    @Nullable
    private String reconcileInput(CollectorsConfigRequest.IngestEndpointRequest requested,
                                  @Nullable IngestEndpointConfig existing,
                                  String inputType,
                                  String title) {
        final String existingInputId = existing != null ? existing.inputId() : null;
        final boolean inputExists = existingInputId != null && inputExistsInDb(existingInputId);

        if (requested.enabled()) {
            if (!inputExists) {
                return createManagedInput(inputType, title);
            }
            // Trigger restart if port changed so transport picks up new config
            if (existing.port() != requested.port()) {
                restartInput(existingInputId);
            }
            return existingInputId;
        } else {
            if (inputExists) {
                deleteManagedInput(existingInputId);
            }
            return null;
        }
    }

    private boolean inputExistsInDb(String inputId) {
        try {
            inputService.find(inputId);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    private String createManagedInput(String inputType, String title) {
        final String creatorUserId = SecurityUtils.getSubject().getPrincipal().toString();
        final var input = inputService.create(Map.of(
                MessageInput.FIELD_TYPE, inputType,
                MessageInput.FIELD_TITLE, title,
                MessageInput.FIELD_CREATOR_USER_ID, creatorUserId,
                MessageInput.FIELD_GLOBAL, true,
                MessageInput.FIELD_CONFIGURATION, Map.of(),
                MessageInput.FIELD_DESIRED_STATE, IOState.Type.RUNNING.name()
        ));
        try {
            return inputService.save(input);
        } catch (ValidationException e) {
            throw new InternalServerErrorException("Failed to create managed input: " + title, e);
        }
    }

    private void restartInput(String inputId) {
        try {
            final Input input = inputService.find(inputId);
            inputService.update(input);
        } catch (NotFoundException e) {
            LOG.warn("Input {} not found during restart attempt", inputId);
        } catch (ValidationException e) {
            throw new InternalServerErrorException("Failed to restart input " + inputId, e);
        }
    }

    private void deleteManagedInput(String inputId) {
        try {
            final Input input = inputService.find(inputId);
            inputService.destroy(input);
        } catch (NotFoundException e) {
            LOG.warn("Input {} not found during delete attempt", inputId);
        }
    }
}
