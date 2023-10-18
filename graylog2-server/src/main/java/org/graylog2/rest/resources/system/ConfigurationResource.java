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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.Configuration;
import org.graylog2.configuration.ExposedConfiguration;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;

@RequiresAuthentication
@Api(value = "System/Configuration", description = "Read-only access to configuration settings")
@Path("/system/configuration")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationResource extends RestResource {
    private final Configuration configuration;

    @Inject
    public ConfigurationResource(final Configuration configuration) {
        this.configuration = configuration;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get relevant configuration settings and their values")
    public ExposedConfiguration getRelevant() {
        return ExposedConfiguration.create(configuration);
    }

    @GET
    @Path("/{name}")
    @Timed
    @ApiOperation(value = "Get relevant configuration setting value")
    public Response getRelevantByName(@ApiParam(name = "name", required = true)
                                      @PathParam("name") @NotEmpty String configSettingName) {

        final ExposedConfiguration conf = ExposedConfiguration.create(configuration);

        return Arrays.stream(ExposedConfiguration.class.getMethods())
                .filter(method -> method.isAnnotationPresent(JsonProperty.class))
                .filter(method -> method.getAnnotation(JsonProperty.class).value().equals(configSettingName))
                .findFirst()
                .map(method -> {
                    try {
                        return Response.ok(method.invoke(conf)).build();
                    } catch (Exception ex) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getMessage()).build();
                    }
                }).orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
}
