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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplate;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.io.IOException;
import java.util.Optional;

@RequiresAuthentication
@Api(value = "System/IndexSetDefaults", description = "Index set defaults")
@Path("/system/indices/index_set_defaults")
@Produces(MediaType.APPLICATION_JSON)
public class IndexSetDefaultsResource extends RestResource {
    private final IndexSetDefaultTemplateService indexSetDefaultTemplateService;

    @Inject
    public IndexSetDefaultsResource(IndexSetDefaultTemplateService indexSetDefaultTemplateService) {
        this.indexSetDefaultTemplateService = indexSetDefaultTemplateService;
    }

    @PUT
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Set index set default template")
    @RequiresPermissions({RestPermissions.CLUSTER_CONFIG_ENTRY_CREATE, RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT})
    @NoAuditEvent("event is handled in service class")
    public Response update(@ApiParam(name = "body", value = "Index set default template id.", required = true)
                           @NotNull IndexSetDefaultTemplate defaultTemplate) throws IOException {
        try {
            indexSetDefaultTemplateService.setDefault(defaultTemplate, Optional.ofNullable(getCurrentUser())
                    .map(User::getName)
                    .orElse("unknown"));
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
        return Response.ok().build();
    }
}
