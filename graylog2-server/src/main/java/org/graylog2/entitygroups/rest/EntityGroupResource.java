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
package org.graylog2.entitygroups.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.entitygroups.EntityGroupService;
import org.graylog2.entitygroups.model.EntityCategory;
import org.graylog2.database.PaginatedList;
import org.graylog2.entitygroups.model.EntityGroup;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.shared.rest.resources.RestResource;

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

@Api(value = "Entity Groups", description = "Manage Entity Groups")
@Path("/entity_groups")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
public class EntityGroupResource extends RestResource implements PluginRestResource {

    private final EntityGroupService entityGroupService;

    @Inject
    public EntityGroupResource(EntityGroupService entityGroupService) {
        this.entityGroupService = entityGroupService;
    }

    @GET
    @ApiOperation(value = "Get a list of entity groups")
    @RequiresPermissions(EntityGroupPermissions.ENTITY_GROUP_READ)
    public PaginatedList<EntityGroup> listStatuses(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                   @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("15") int perPage,
                                                   @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                   @ApiParam(name = "sort",
                                                             value = "The field to sort the result on",
                                                             required = true)
                                                   @QueryParam("sort") String sort,
                                                   @ApiParam(name = "direction", value = "The sort direction", allowableValues = "asc,desc")
                                                   @DefaultValue("asc") @QueryParam("direction") SortOrder order) {

        return entityGroupService.findPaginated(query, page, perPage, order, sort, null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Create a new entity group")
    @RequiresPermissions(EntityGroupPermissions.ENTITY_GROUP_EDIT)
    @AuditEvent(type = AuditEventTypes.ENTITY_GROUP_CREATE)
    public Response create(@ApiParam(name = "JSON Body") EntityGroup entityGroup) {
        return Response.ok().entity(entityGroupService.create(entityGroup)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Update an entity group")
    @RequiresPermissions(EntityGroupPermissions.ENTITY_GROUP_EDIT)
    @AuditEvent(type = AuditEventTypes.ENTITY_GROUP_UPDATE)
    public Response update(@ApiParam(name = "id", required = true) @PathParam("id") String id,
                           @ApiParam(name = "JSON Body") EntityGroup entityGroup) {
        return Response.ok().entity(entityGroupService.update(id, entityGroup)).build();
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Delete an entity group")
    @RequiresPermissions(EntityGroupPermissions.ENTITY_GROUP_EDIT)
    @AuditEvent(type = AuditEventTypes.ENTITY_GROUP_DELETE)
    public Response delete(@ApiParam(name = "id", required = true) @PathParam("id") String id) {
        entityGroupService.delete(id);
        return Response.ok().build();
    }
}
