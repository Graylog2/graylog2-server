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
package org.graylog2.rest.resources.entityscope;

import com.google.common.base.Objects;
import com.mongodb.MongoException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.entityscope.EntityScope;
import org.graylog2.entityscope.EntityScopeDbService;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Api(value = "EntityScopes", description = "Manage Entity Scopes")
@Path("entity_scopes")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
public class EntityScopeResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(EntityScopeResource.class.getSimpleName());
    private final EntityScopeDbService dbService;

    @Inject
    public EntityScopeResource(EntityScopeDbService dbService) {
        this.dbService = dbService;
    }

    @GET
    @ApiOperation("Get all Entity Scopes")
    public PaginatedResponse<EntityScope> getAllEntityScopes(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                             @ApiParam(name = "page_size") @QueryParam("page_size") @DefaultValue("50") int pageSize) {
        try {
            PaginatedList<EntityScope> list = dbService.findAll(page, pageSize);
            return PaginatedResponse.create("entities", list);
        } catch (MongoException e) {
            throw new ServerErrorException("Error reading database", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("{id}")
    public EntityScope getById(@ApiParam("id") @PathParam("id") String id) {
        try {
            return dbService.get(id)
                    .orElseThrow(() -> new NotFoundException("Entity scope not found. " + id));
        } catch (MongoException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerErrorException("Error reading database", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @ApiOperation("Create a new Entity Scope")
    @AuditEvent(type = AuditEventTypes.ENTITY_SCOPE_CREATE)
    public EntityScope createEntityScope(@ApiParam("entityScope") EntityScopeRequest request) {

        try {
            dbService.findByName(request.title())
                    .ifPresent(e -> {
                        throw new BadRequestException("Entity Scope already exists. " + request.title());
                    });
            return dbService.save(request.toEntity());
        } catch (MongoException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerErrorException("Error saving scoped entity. ", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("{id}")
    @ApiOperation("Update a new Entity Scope")
    @AuditEvent(type = AuditEventTypes.ENTITY_SCOPE_UPDATE)
    public EntityScope updateEntityScope(@ApiParam("id") @PathParam("id") String id,
                                         @ApiParam("entityScope") EntityScopeRequest request) {

        try {

            // Assert the name was not changed to name of other scope record
            dbService.findByName(request.title())
                    .filter(e -> !Objects.equal(e.id(), id))
                    .ifPresent(e -> {
                        throw new BadRequestException("Entity Scope already exists. " + request.title());
                    });

            EntityScope current = dbService.get(id).orElseThrow(() -> new BadRequestException("Entity Scope not found. " + id));
            EntityScope dirty = request.merge(current);

            return dbService.save(dirty);
        } catch (MongoException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerErrorException("Error saving scoped entity. ", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("{id}")
    @ApiOperation("Delete an existing Entity Scope")
    @AuditEvent(type = AuditEventTypes.ENTITY_SCOPE_DELETE)
    public void delete(@ApiParam("id") @PathParam("id") String id) {
        try {
            EntityScope current = dbService.get(id)
                    .orElseThrow(() -> new NotFoundException("Entity scope not found. " + id));
            dbService.delete(current.id());
        } catch (MongoException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerErrorException("Error while deleting entity scope", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
