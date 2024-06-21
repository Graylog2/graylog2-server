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

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.entitygroups.EntityGroupAuditEventTypes;
import org.graylog2.entitygroups.EntityGroupService;
import org.graylog2.entitygroups.model.EntityGroup;
import org.graylog2.database.PaginatedList;
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
import org.graylog2.shared.utilities.StringUtils;

import java.util.function.Predicate;

import static org.graylog2.entitygroups.rest.EntityGroupPermissions.ENTITY_GROUP_CREATE;
import static org.graylog2.entitygroups.rest.EntityGroupPermissions.ENTITY_GROUP_DELETE;
import static org.graylog2.entitygroups.rest.EntityGroupPermissions.ENTITY_GROUP_EDIT;
import static org.graylog2.entitygroups.rest.EntityGroupPermissions.ENTITY_GROUP_READ;
import static org.graylog2.shared.utilities.ExceptionUtils.getRootCauseMessage;

@Api(value = "EntityGroups", description = "Manage Entity Groups")
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
    public PaginatedList<EntityGroup> listGroups(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                 @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("15") int perPage,
                                                 @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                 @ApiParam(name = "sort",
                                                           value = "The field to sort the result on",
                                                           allowableValues = "name")
                                                 @DefaultValue(EntityGroup.FIELD_NAME)
                                                 @QueryParam("sort") String sort,
                                                 @ApiParam(name = "direction", value = "The sort direction", allowableValues = "asc,desc")
                                                 @DefaultValue("asc") @QueryParam("direction") SortOrder order) {
        final Predicate<EntityGroup> filter = entityGroup -> isPermitted(ENTITY_GROUP_READ, entityGroup.id());
        return entityGroupService.findPaginated(query, page, perPage, order, sort, filter);
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get a single entity group")
    @ApiResponses(@ApiResponse(code = 404, message = "No such entity group"))
    public Response get(@ApiParam(name = "id", required = true) @PathParam("id") String id) {
        if (!isPermitted(ENTITY_GROUP_READ, id)) {
            throw new ForbiddenException("Not allowed to read group id " + id);
        }
        try {
            return Response.ok().entity(entityGroupService.requireEntityGroup(id)).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(getRootCauseMessage(e), e);
        }
    }

    @GET
    @Path("/get_for_entity")
    @ApiOperation(value = "Get a list of entity groups for an entity")
    public PaginatedList<EntityGroup> listGroupsForEntity(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                          @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("15") int perPage,
                                                          @ApiParam(name = "entity_type", required = true) @QueryParam("entity_type") String entityType,
                                                          @ApiParam(name = "entity_id", required = true) @QueryParam("entity_id") String entityId,
                                                          @ApiParam(name = "sort",
                                                                    value = "The field to sort the result on",
                                                                    allowableValues = "name")
                                                          @DefaultValue(EntityGroup.FIELD_NAME)
                                                          @QueryParam("sort") String sort,
                                                          @ApiParam(name = "direction", value = "The sort direction", allowableValues = "asc,desc")
                                                          @DefaultValue("asc") @QueryParam("direction") SortOrder order) {
        final Predicate<EntityGroup> filter = entityGroup -> isPermitted(ENTITY_GROUP_READ, entityGroup.id());
        return entityGroupService.findPaginatedForEntity(entityType, entityId, page, perPage, order, sort, filter);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Create a new entity group")
    @ApiResponses(@ApiResponse(code = 400, message = "An entity group already exists with id or name"))
    @RequiresPermissions(ENTITY_GROUP_CREATE)
    @AuditEvent(type = EntityGroupAuditEventTypes.ENTITY_GROUP_CREATE)
    public Response create(@ApiParam(name = "JSON Body") EntityGroupRequest request) {
        try {
            return Response.ok().entity(entityGroupService.create(request.toEntityGroup())).build();
        } catch (DuplicateKeyException | MongoWriteException e) {
            throw new BadRequestException(StringUtils.f("Entity group '%s' already exists", request.name()));
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Update an entity group")
    @ApiResponses({
            @ApiResponse(code = 400, message = "An entity group already exists with id or name"),
            @ApiResponse(code = 404, message = "No such entity group")
    })
    @AuditEvent(type = EntityGroupAuditEventTypes.ENTITY_GROUP_UPDATE)
    public Response update(@ApiParam(name = "id", required = true) @PathParam("id") String id,
                           @ApiParam(name = "JSON Body") EntityGroupRequest request) {
        if (!isPermitted(ENTITY_GROUP_EDIT, id)) {
            throw new ForbiddenException("Not allowed to edit group id " + id);
        }
        try {
            return Response.ok().entity(entityGroupService.update(id, request.toEntityGroup())).build();
        } catch (IllegalArgumentException e) {
            throw new NotFoundException(getRootCauseMessage(e), e);
        } catch (DuplicateKeyException | MongoWriteException e) {
            throw new BadRequestException(StringUtils.f("Entity group '%s' already exists", request.name()));
        }
    }

    @PUT
    @Path("/{group_id}/add_entity")
    @ApiOperation("Add an entity to an entity group")
    @ApiResponses(@ApiResponse(code = 404, message = "No such entity group"))
    @AuditEvent(type = EntityGroupAuditEventTypes.ENTITY_GROUP_ADD_ENTITY)
    public Response addEntityToGroup(@ApiParam(name = "group_id", required = true) @PathParam("group_id") String groupId,
                                     @ApiParam(name = "entity_type", required = true) @QueryParam("entity_type") String entityType,
                                     @ApiParam(name = "entity_id", required = true) @QueryParam("entity_id") String entityId) {
        if (!isPermitted(ENTITY_GROUP_EDIT, groupId)) {
            throw new ForbiddenException("Not allowed to edit group id " + groupId);
        }

        try {
            return Response.ok().entity(entityGroupService.addEntityToGroup(groupId, entityType, entityId)).build();
        } catch (IllegalArgumentException e) {
            throw new NotFoundException(getRootCauseMessage(e), e);
        }
    }

    @PUT
    @Path("/get_for_entities")
    @ApiOperation("Get a list of entity groups for a list of entities")
    @RequiresPermissions(ENTITY_GROUP_READ)
    @NoAuditEvent("Read resource - doesn't change any data")
    public BulkEntityGroupResponse getAllForEntity(@ApiParam(name = "JSON Body") BulkEntityGroupRequest request) {
        return new BulkEntityGroupResponse(entityGroupService.getAllForEntities(request.type(), request.entityIds()));
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Delete an entity group")
    @AuditEvent(type = EntityGroupAuditEventTypes.ENTITY_GROUP_DELETE)
    public void delete(@ApiParam(name = "id", required = true) @PathParam("id") String id) {
        if (!isPermitted(ENTITY_GROUP_DELETE, id)) {
            throw new ForbiddenException("Not allowed to delete group id " + id);
        }
        entityGroupService.delete(id);
    }
}
