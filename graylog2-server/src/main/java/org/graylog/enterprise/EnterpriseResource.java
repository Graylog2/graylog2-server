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
package org.graylog.enterprise;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

@Api(value = "Enterprise")
@Path("/enterprise")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EnterpriseResource extends RestResource {
    private final EnterpriseService enterpriseService;

    @Inject
    public EnterpriseResource(EnterpriseService enterpriseService) {
        this.enterpriseService = enterpriseService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get Graylog Enterprise license info")
    @Path("/license/info")
    @RequiresPermissions(RestPermissions.LICENSEINFOS_READ)
    public Response licenseInfo() {
        return Response.ok(Collections.singletonMap("license_info", enterpriseService.licenseInfo())).build();
    }
}
