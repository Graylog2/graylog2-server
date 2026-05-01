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
package org.graylog.events.tags.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
import org.graylog.events.audit.EventsAuditEventTypes;
import org.graylog.events.tags.TagService;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;

@Tag(name = "Events/Tags", description = "Manage event definition tags")
@Path("/events/tags")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
public class TagResource extends RestResource implements PluginRestResource {

    private final TagService tagService;

    @Inject
    public TagResource(TagService tagService) {
        this.tagService = tagService;
    }

    @GET
    @Operation(summary = "Get a paginated list of event definition tags")
    @RequiresPermissions(RestPermissions.EVENT_DEFINITIONS_READ)
    public PaginatedList<org.graylog.events.tags.Tag> list(
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("15") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "sort", description = "The field to sort the result on", schema = @Schema(allowableValues = {"value"}))
            @DefaultValue(org.graylog.events.tags.Tag.FIELD_VALUE) @QueryParam("sort") String sort,
            @Parameter(name = "direction", description = "The sort direction", schema = @Schema(allowableValues = {"asc", "desc"}))
            @DefaultValue("asc") @QueryParam("direction") SortOrder order) {

        return tagService.findPaginated(query, page, perPage, order, sort, null);
    }

    @GET
    @Path("/all")
    @Operation(summary = "Get all event definition tags")
    @RequiresPermissions(RestPermissions.EVENT_DEFINITIONS_READ)
    @NoAuditEvent("Not changing any data.")
    public List<org.graylog.events.tags.Tag> getAll() {
        return tagService.getAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new event definition tag")
    @RequiresPermissions(RestPermissions.EVENT_DEFINITIONS_EDIT)
    @AuditEvent(type = EventsAuditEventTypes.EVENT_TAG_CREATE)
    public Response create(@Parameter(name = "Tag value") String value) {
        return Response.ok().entity(tagService.create(value)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update an event definition tag")
    @RequiresPermissions(RestPermissions.EVENT_DEFINITIONS_EDIT)
    @AuditEvent(type = EventsAuditEventTypes.EVENT_TAG_UPDATE)
    public Response update(@Parameter(name = "id", required = true) @PathParam("id") String id,
                           @Parameter(name = "Tag value") String value) {
        return Response.ok().entity(tagService.update(id, value)).build();
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete an event definition tag")
    @RequiresPermissions(RestPermissions.EVENT_DEFINITIONS_EDIT)
    @AuditEvent(type = EventsAuditEventTypes.EVENT_TAG_DELETE)
    public Response delete(@Parameter(name = "id", required = true) @PathParam("id") String id) {
        tagService.delete(id);
        return Response.ok().build();
    }
}
