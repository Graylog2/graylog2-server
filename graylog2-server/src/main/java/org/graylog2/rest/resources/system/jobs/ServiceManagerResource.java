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
package org.graylog2.rest.resources.system.jobs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@RequiresAuthentication
@Api(value = "System/ServiceManager", description = "ServiceManager Status")
@Path("/system/serviceManager")
public class ServiceManagerResource extends RestResource {
    private final ServiceManager serviceManager;

    @Inject
    public ServiceManagerResource(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    @GET
    @Timed
    @ApiOperation(value = "List current status of ServiceManager")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Service, Long> list() {
        return serviceManager.startupTimes();
    }
}
