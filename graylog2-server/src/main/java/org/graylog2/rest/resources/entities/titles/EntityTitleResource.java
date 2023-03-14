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
package org.graylog2.rest.resources.entities.titles;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.rest.resources.entities.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.entities.titles.model.EntityTitleRequest;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "EntitiesTitles", tags = {CLOUD_VISIBLE})
@Path("/entity_titles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EntityTitleResource extends RestResource {

    private final EntityTitleService entityTitleService;

    @Inject
    public EntityTitleResource(final EntityTitleService entityTitleService) {
        this.entityTitleService = entityTitleService;
    }

    @POST
    @Timed
    @ApiOperation(value = "Get titles of provided entities")
    @NoAuditEvent("This endpoint does not change any data")
    public EntitiesTitleResponse getTitles(@ApiParam(name = "JSON body", required = true) final EntityTitleRequest request) {
        return entityTitleService.getTitles(request);
    }
}
