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

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.SourceDTO;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQuery;
import org.graylog2.shared.rest.resources.RestResource;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Tag(name = "Collectors/Sources")
@Path("/collectors/fleets/{fleetId}/sources")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SourceResource extends RestResource {

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id("name").title("Name").build(),
            EntityAttribute.builder().id("description").title("Description").build(),
            EntityAttribute.builder().id("type").title("Type").sortable(false).build(),
            EntityAttribute.builder().id("enabled").title("Enabled").sortable(false).build()
    );

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create("name", Sorting.Direction.ASC))
            .build();

    private final SourceService sourceService;
    private final AuditEventSender auditEventSender;

    @Inject
    public SourceResource(SourceService sourceService, AuditEventSender auditEventSender) {
        this.sourceService = sourceService;
        this.auditEventSender = auditEventSender;
    }

    @GET
    @Timed
    @Operation(summary = "Get a paginated list of sources for a fleet")
    public PageListResponse<SourceResponse> list(
            @Parameter(name = "fleetId", required = true) @PathParam("fleetId") String fleetId,
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "sort") @QueryParam("sort") @DefaultValue("name") String sort,
            @Parameter(name = "order") @QueryParam("order") @DefaultValue("asc") SortOrder order) {

        checkPermission(CollectorsPermissions.FLEET_READ, fleetId);
        final SearchQuery searchQuery;
        try {
            searchQuery = sourceService.parseSearchQuery(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid search query: " + e.getMessage(), e);
        }

        final PaginatedList<SourceDTO> result = sourceService.findByFleet(fleetId, searchQuery, page, perPage, sort, order);

        return PageListResponse.create(
                query,
                result.pagination(),
                result.pagination().total(),
                sort,
                order,
                result.stream().map(SourceResponse::fromDTO).toList(),
                ATTRIBUTES,
                DEFAULTS);
    }

    @GET
    @Path("/{sourceId}")
    @Timed
    @Operation(summary = "Get a single source")
    public SourceResponse get(
            @Parameter(name = "fleetId", required = true) @PathParam("fleetId") String fleetId,
            @Parameter(name = "sourceId", required = true) @PathParam("sourceId") String sourceId) {
        checkPermission(CollectorsPermissions.FLEET_READ, fleetId);
        return sourceService.get(fleetId, sourceId)
                .map(SourceResponse::fromDTO)
                .orElseThrow(() -> new NotFoundException("Source " + sourceId + " not found"));
    }

    @POST
    @Timed
    @Operation(summary = "Create a new source in a fleet")
    @AuditEvent(type = AuditEventTypes.COLLECTOR_SOURCE_CREATE)
    public Response create(
            @Parameter(name = "fleetId", required = true) @PathParam("fleetId") String fleetId,
            @Valid @NotNull @RequestBody(required = true, useParameterTypeSchema = true) CreateSourceRequest request) {
        checkPermission(CollectorsPermissions.SOURCE_CREATE, fleetId);
        final SourceDTO created;
        try {
            created = sourceService.create(fleetId, request.name(), request.description(), request.enabled(), request.config());
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("message", e.getMessage()))
                        .build();
            }
            throw new BadRequestException(e.getMessage(), e);
        }

        final SourceResponse response = SourceResponse.fromDTO(created);
        final URI uri = getUriBuilderToSelf().path(SourceResource.class, "get")
                .build(fleetId, created.id());
        return Response.created(uri).entity(response).build();
    }

    @PUT
    @Path("/{sourceId}")
    @Timed
    @Operation(summary = "Update a source")
    @AuditEvent(type = AuditEventTypes.COLLECTOR_SOURCE_UPDATE)
    public Response update(
            @Parameter(name = "fleetId", required = true) @PathParam("fleetId") String fleetId,
            @Parameter(name = "sourceId", required = true) @PathParam("sourceId") String sourceId,
            @Valid @NotNull @RequestBody(required = true, useParameterTypeSchema = true) UpdateSourceRequest request) {
        checkPermission(CollectorsPermissions.SOURCE_EDIT, fleetId);
        try {
            return sourceService.update(fleetId, sourceId, request.name(), request.description(), request.enabled(), request.config())
                    .map(SourceResponse::fromDTO)
                    .map(response -> Response.ok(response).build())
                    .orElseThrow(() -> new NotFoundException("Source " + sourceId + " not found"));
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("message", e.getMessage()))
                        .build();
            }
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    @DELETE
    @Path("/{sourceId}")
    @Timed
    @Operation(summary = "Delete a source")
    @NoAuditEvent("inline")
    public void delete(
            @Parameter(name = "fleetId", required = true) @PathParam("fleetId") String fleetId,
            @Parameter(name = "sourceId", required = true) @PathParam("sourceId") String sourceId) {
        checkPermission(CollectorsPermissions.SOURCE_DELETE, fleetId);
        final var source = sourceService.get(fleetId, sourceId)
                .orElseThrow(() -> new NotFoundException("Source " + sourceId + " not found"));
        if (!sourceService.delete(fleetId, sourceId)) {
            throw new NotFoundException("Source " + sourceId + " not found");
        }
        auditEventSender.success(AuditActor.user(getCurrentUser()), AuditEventTypes.COLLECTOR_SOURCE_DELETE, Map.of(
                "sourceId", Objects.requireNonNull(source.id()),
                "fleetId", fleetId,
                "name", source.name()
        ));
    }
}
