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
package org.graylog2.plugin.quickjump.rest;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.security.UserContext;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.quickjump.QuickJumpService;
import org.graylog2.plugin.rest.PluginRestResource;

import java.io.IOException;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "QuickJump", description = "Quick Jump Functionality", tags = {CLOUD_VISIBLE})
@Path("/quickjump")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class QuickJumpResource implements PluginRestResource {
    private final QuickJumpService quickJumpService;

    @Inject
    public QuickJumpResource(QuickJumpService quickJumpService) {
        this.quickJumpService = quickJumpService;
    }

    @POST
    @ApiOperation(value = "Returns results for existing entities based on supplied query")
    @NoAuditEvent("Not changing any data")
    @Timed
    public QuickJumpResponse search(@ApiParam(name = "JSON Body") @Valid QuickJumpRequest request,
                                    @Context UserContext userContext) throws IOException {
        return quickJumpService.search(request.query(), userContext);
    }
}
