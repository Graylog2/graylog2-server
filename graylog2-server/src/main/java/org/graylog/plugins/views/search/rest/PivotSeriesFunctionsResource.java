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
package org.graylog.plugins.views.search.rest;

import io.swagger.annotations.Api;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Api(value = "Search/Functions")
@Path("/views/functions")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class PivotSeriesFunctionsResource extends RestResource implements PluginRestResource {
    private final Map<String, SeriesDescription> availableFunctions;

    @Inject
    public PivotSeriesFunctionsResource(Map<String, SeriesDescription> availableFunctions) {
        this.availableFunctions = availableFunctions;
    }

    @GET
    public Map<String, SeriesDescription> functions() {
        return this.availableFunctions;
    }
}
