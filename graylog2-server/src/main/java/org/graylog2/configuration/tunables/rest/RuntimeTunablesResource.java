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
package org.graylog2.configuration.tunables.rest;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.configuration.tunables.RuntimeTunableDescriptor;
import org.graylog2.configuration.tunables.RuntimeTunableFeature;
import org.graylog2.configuration.tunables.RuntimeTunableRegistry;
import org.graylog2.configuration.tunables.RuntimeTunableService;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.List;

import static org.graylog2.shared.utilities.StringUtils.f;

@Tag(name = "System/RuntimeTunables", description = "Runtime tunables grouped by feature")
@RequiresAuthentication
@Path("/system/runtime_tunables")
@Produces(MediaType.APPLICATION_JSON)
public class RuntimeTunablesResource extends RestResource {
    private final RuntimeTunableRegistry registry;
    private final RuntimeTunableService service;
    private final ObjectMapper objectMapper;

    @Inject
    public RuntimeTunablesResource(RuntimeTunableRegistry registry, RuntimeTunableService service, ObjectMapper objectMapper) {
        this.registry = registry;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @GET
    @Timed
    @Operation(summary = "List features with the number of readable runtime tunables")
    public RuntimeTunablesResponse list() {
        return new RuntimeTunablesResponse(registry.features().stream()
                .map(feature -> new RuntimeTunableFeatureSummary(feature.id(), feature.title(), feature.order(),
                        readableTunables(feature).size()))
                .toList());
    }

    @GET
    @Timed
    @Path("features/{feature_id}")
    @Operation(summary = "Get the readable runtime tunables of a feature")
    public RuntimeTunableFeatureResponse getFeature(@Parameter(name = "feature_id", required = true) @PathParam("feature_id") String featureId) {
        final RuntimeTunableFeature feature = registry.features().stream()
                .filter(candidate -> candidate.id().equals(featureId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(f("Couldn't find runtime tunable feature \"%s\"", featureId)));
        return new RuntimeTunableFeatureResponse(feature.id(), feature.title(), feature.order(),
                readableTunables(feature).stream().map(this::toResponse).toList());
    }

    @PUT
    @Timed
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a runtime tunable")
    @AuditEvent(type = AuditEventTypes.CLUSTER_CONFIGURATION_UPDATE, captureRequestEntity = false)
    public Response update(@Parameter(name = "id", required = true) @PathParam("id") String id,
                           @Parameter(name = "body", required = true) @NotNull JsonNode body) {
        final RuntimeTunableDescriptor descriptor = editable(id);
        if (body == null || body.isNull()) {
            throw new BadRequestException(f("Missing configuration for runtime tunable \"%s\"", id));
        }
        final Object config;
        try {
            config = objectMapper.treeToValue(body, descriptor.type());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new BadRequestException(f("Couldn't parse runtime tunable \"%s\": %s", id, e.getMessage()));
        }
        service.update(descriptor, config);
        return Response.accepted(toResponse(descriptor)).build();
    }

    @DELETE
    @Timed
    @Path("{id}")
    @Operation(summary = "Reset a runtime tunable to its defaults")
    @AuditEvent(type = AuditEventTypes.CLUSTER_CONFIGURATION_DELETE)
    public RuntimeTunableResponse reset(@Parameter(name = "id", required = true) @PathParam("id") String id) {
        final RuntimeTunableDescriptor descriptor = editable(id);
        service.reset(descriptor);
        return toResponse(descriptor);
    }

    private List<RuntimeTunableDescriptor> readableTunables(RuntimeTunableFeature feature) {
        return registry.tunables(feature.id()).stream()
                .filter(descriptor -> isPermitted(descriptor.readPermission()))
                .toList();
    }

    private RuntimeTunableDescriptor editable(String id) {
        final RuntimeTunableDescriptor descriptor = registry.byId(id)
                .orElseThrow(() -> new NotFoundException(f("Couldn't find runtime tunable \"%s\"", id)));
        checkPermission(descriptor.readPermission());
        checkPermission(descriptor.editPermission());
        return descriptor;
    }

    private RuntimeTunableResponse toResponse(RuntimeTunableDescriptor descriptor) {
        final RuntimeTunableService.Value value = service.read(descriptor);
        return new RuntimeTunableResponse(descriptor.id(), descriptor.title(), descriptor.description(), descriptor.schema(),
                value.value(), descriptor.defaults(), value.isDefault(), isPermitted(descriptor.editPermission()));
    }
}
