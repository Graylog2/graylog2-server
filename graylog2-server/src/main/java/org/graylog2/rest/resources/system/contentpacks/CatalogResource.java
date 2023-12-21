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
package org.graylog2.rest.resources.system.contentpacks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.rest.models.system.contentpacks.responses.CatalogIndexResponse;
import org.graylog2.rest.models.system.contentpacks.responses.CatalogResolveRequest;
import org.graylog2.rest.models.system.contentpacks.responses.CatalogResolveResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.EntityTitleService;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleRequest;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import jakarta.inject.Inject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.Set;

@RequiresAuthentication
@Api(value = "System/Catalog", description = "Entity Catalog")
@Path("/system/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource extends RestResource {
    private final ContentPackService contentPackService;
    private final EntityTitleService entityTitleService;

    @Inject
    public CatalogResource(final ContentPackService contentPackService,
                           final EntityTitleService entityTitleService) {
        this.contentPackService = contentPackService;
        this.entityTitleService = entityTitleService;
    }

    @GET
    @Timed
    @ApiOperation(value = "List available entities in this Graylog cluster")
    @RequiresPermissions(RestPermissions.CATALOG_LIST)
    public CatalogIndexResponse showEntityIndex() {
        final Set<EntityExcerpt> entities = contentPackService.listAllEntityExcerpts();
        return CatalogIndexResponse.create(entities);
    }

    @POST
    @Timed
    @ApiOperation(value = "Resolve dependencies of entities and return their configuration")
    @RequiresPermissions(RestPermissions.CATALOG_RESOLVE)
    @NoAuditEvent("this is not changing any data")
    public CatalogResolveResponse resolveEntities(
            @ApiParam(name = "JSON body", required = true)
            @Valid @NotNull CatalogResolveRequest request) {
        final Set<EntityDescriptor> requestedEntities = request.entities();
        final Set<EntityDescriptor> resolvedEntities = contentPackService.resolveEntities(requestedEntities);
        final ImmutableSet<Entity> entities = contentPackService.collectEntities(resolvedEntities);

        return CatalogResolveResponse.create(entities);
    }

    @POST
    @Timed
    @ApiOperation(value = "Get titles of provided entities")
    @NoAuditEvent("This endpoint does not change any data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/entities/titles")
    public EntitiesTitleResponse getTitles(@ApiParam(name = "JSON body", required = true) final EntityTitleRequest request, @Context SearchUser searchUser) {
        return entityTitleService.getTitles(request, searchUser);
    }
}
