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
import org.graylog.collectors.FleetService;
import org.graylog.collectors.db.FleetDTO;
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

    @Inject
    public FleetResource(FleetService fleetService) {
        this.fleetService = fleetService;
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
                fleet -> isPermitted(FleetPermissions.FLEET_READ, fleet.id()));

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
    public FleetResponse get(@PathParam("fleetId") String fleetId) {
        checkPermission(FleetPermissions.FLEET_READ, fleetId);
        // TODO: audit event
        return fleetService.get(fleetId)
                .map(FleetResponse::fromDTO)
                .orElseThrow(() -> new NotFoundException("Fleet " + fleetId + " not found"));
    }

    @POST
    @Timed
    @Operation(summary = "Create a new fleet")
    @RequiresPermissions(FleetPermissions.FLEET_CREATE)
    public Response create(@Valid @NotNull CreateFleetRequest request) {
        // TODO: audit event
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
    public FleetResponse update(@PathParam("fleetId") String fleetId, @Valid @NotNull UpdateFleetRequest request) {
        checkPermission(FleetPermissions.FLEET_EDIT, fleetId);
        // TODO: audit event
        return fleetService.update(fleetId, request.name(), request.description(), request.targetVersion())
                .map(FleetResponse::fromDTO)
                .orElseThrow(() -> new NotFoundException("Fleet " + fleetId + " not found"));
    }

    @DELETE
    @Path("/{fleetId}")
    @Timed
    @Operation(summary = "Delete a fleet")
    public void delete(@PathParam("fleetId") String fleetId) {
        checkPermission(FleetPermissions.FLEET_DELETE, fleetId);
        // TODO: audit event
        if (!fleetService.delete(fleetId)) {
            throw new NotFoundException("Fleet " + fleetId + " not found");
        }
    }
}
