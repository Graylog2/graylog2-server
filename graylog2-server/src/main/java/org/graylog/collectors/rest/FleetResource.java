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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.FleetDTO;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Tag(name = "Collectors/Fleets")
@Path("/collectors/fleets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FleetResource extends RestResource {

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id("name").title("Name").build(),
            EntityAttribute.builder().id("description").title("Description").build()
    );

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create("name", Sorting.Direction.ASC))
            .build();

    private final FleetService fleetService;
    private final CollectorInstanceService instanceService;
    private final SourceService sourceService;
    private final CollectorsConfigService collectorsConfigService;

    @Inject
    public FleetResource(FleetService fleetService, CollectorInstanceService instanceService,
                         SourceService sourceService, CollectorsConfigService collectorsConfigService) {
        this.fleetService = fleetService;
        this.instanceService = instanceService;
        this.sourceService = sourceService;
        this.collectorsConfigService = collectorsConfigService;
    }

    @GET
    @Timed
    @Operation(summary = "Get a paginated list of fleets")
    public PageListResponse<FleetResponse> list(
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "sort") @QueryParam("sort") @DefaultValue("name") String sort,
            @Parameter(name = "order") @QueryParam("order") @DefaultValue("asc") SortOrder order) {

        final SearchQuery searchQuery;
        try {
            searchQuery = fleetService.parseSearchQuery(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid search query: " + e.getMessage(), e);
        }

        final PaginatedList<FleetDTO> result = fleetService.findPaginated(searchQuery, page, perPage, sort, order,
                fleet -> isPermitted(CollectorsPermissions.FLEET_READ, fleet.id()));

        return PageListResponse.create(
                query,
                result.pagination(),
                result.pagination().total(),
                sort,
                order,
                result.stream().map(FleetResponse::fromDTO).toList(),
                ATTRIBUTES,
                DEFAULTS);
    }

    @GET
    @Path("/{fleetId}")
    @Timed
    @Operation(summary = "Get a single fleet")
    public FleetResponse get(@Parameter(name = "fleetId", required = true) @PathParam("fleetId") String fleetId) {
        checkPermission(CollectorsPermissions.FLEET_READ, fleetId);
        // TODO: audit event
        return fleetService.get(fleetId)
                .map(FleetResponse::fromDTO)
                .orElseThrow(() -> new NotFoundException("Fleet " + fleetId + " not found"));
    }

    @GET
    @Path("/stats")
    @Timed
    @Operation(summary = "Get statistics for all fleets")
    public BulkFleetStatsResponse bulkStats() {
        final var fleets = fleetService.getAllFleets();
        final var instanceCounts = instanceService.countByFleetGrouped(
                Instant.now().minus(getOfflineThreshold()));
        final var sourceCountByFleet = sourceService.countByFleetGrouped();

        final List<BulkFleetStatsResponse.FleetStatsSummary> summaries = fleets.stream()
                .sorted(Comparator.comparing(FleetDTO::name))
                .map(fleet -> {
                    final long[] counts = instanceCounts.getOrDefault(fleet.id(), new long[]{0, 0});
                    return new BulkFleetStatsResponse.FleetStatsSummary(
                            fleet.id(),
                            fleet.name(),
                            counts[0],
                            counts[1],
                            counts[0] - counts[1],
                            sourceCountByFleet.getOrDefault(fleet.id(), 0L));
                })
                .toList();

        return new BulkFleetStatsResponse(summaries);
    }

    @GET
    @Path("/{fleetId}/stats")
    @Timed
    @Operation(summary = "Get statistics for a fleet")
    public FleetStatsResponse stats(@Parameter(name = "fleetId", required = true) @PathParam("fleetId") String fleetId) {
        checkPermission(CollectorsPermissions.FLEET_READ, fleetId);
        if (fleetService.get(fleetId).isEmpty()) {
            throw new NotFoundException("Fleet " + fleetId + " not found");
        }
        final long totalInstances = instanceService.countByFleet(fleetId);
        final long onlineInstances = instanceService.countOnlineByFleet(fleetId,
                Instant.now().minus(getOfflineThreshold()));
        final long totalSources = sourceService.countByFleet(fleetId);
        return new FleetStatsResponse(totalInstances, onlineInstances,
                totalInstances - onlineInstances, totalSources);
    }

    @POST
    @Timed
    @Operation(summary = "Create a new fleet")
    @RequiresPermissions(CollectorsPermissions.FLEET_CREATE)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The newly created fleet", content = @Content(schema = @Schema(implementation = FleetResponse.class))),
    })
    // TODO: audit event
    @NoAuditEvent("todo")
    public Response create(@Valid @NotNull @RequestBody(required = true, useParameterTypeSchema = true) CreateFleetRequest request) {
        final FleetDTO created = fleetService.create(request.name(), request.description(), request.targetVersion());
        final FleetResponse response = FleetResponse.fromDTO(created);
        final URI uri = getUriBuilderToSelf().path(FleetResource.class, "get")
                .build(created.id());
        return Response.created(uri).entity(response).build();
    }

    @PUT
    @Path("/{fleetId}")
    @Timed
    @Operation(summary = "Update a fleet")
    // TODO: audit event
    @NoAuditEvent("todo")
    public FleetResponse update(@Parameter(name = "fleetId", required = true) @PathParam("fleetId") String fleetId,
                                @Valid @NotNull @RequestBody(required = true, useParameterTypeSchema = true) UpdateFleetRequest request) {
        checkPermission(CollectorsPermissions.FLEET_EDIT, fleetId);
        return fleetService.update(fleetId, request.name(), request.description(), request.targetVersion())
                .map(FleetResponse::fromDTO)
                .orElseThrow(() -> new NotFoundException("Fleet " + fleetId + " not found"));
    }

    @DELETE
    @Path("/{fleetId}")
    @Timed
    @Operation(summary = "Delete a fleet")
    // TODO: audit event
    @NoAuditEvent("todo")
    public void delete(@Parameter(name = "fleetId", required = true) @PathParam("fleetId") String fleetId) {
        checkPermission(CollectorsPermissions.FLEET_DELETE, fleetId);
        // TODO should this fail if there are still collectors using it? should a replacement fleed be required?
        if (!fleetService.delete(fleetId)) {
            throw new NotFoundException("Fleet " + fleetId + " not found");
        }
    }

    private Duration getOfflineThreshold() {
        return collectorsConfigService.getOrDefault().collectorOfflineThreshold();
    }
}
