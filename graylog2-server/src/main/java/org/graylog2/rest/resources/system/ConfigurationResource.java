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
import org.graylog2.configuration.retrieval.SingleConfigurationValueRetriever;
import org.graylog2.shared.rest.resources.RestResource;

import jakarta.inject.Inject;

import jakarta.validation.constraints.NotEmpty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;

@RequiresAuthentication
@Api(value = "System/Configuration", description = "Read-only access to configuration settings")
@Path("/system/configuration")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationResource extends RestResource {
    private final Configuration configuration;
    private final SingleConfigurationValueRetriever singleConfigurationValueRetriever;

    @Inject
    public ConfigurationResource(final Configuration configuration,
                                 final SingleConfigurationValueRetriever singleConfigurationValueRetriever) {
        this.configuration = configuration;
        this.singleConfigurationValueRetriever = singleConfigurationValueRetriever;
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
        final Optional<Object> value = singleConfigurationValueRetriever.retrieveSingleValue(conf, configSettingName);

        return value.map(v -> Response.ok(new SingleValue(v)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());

    }

    public record SingleValue(@JsonProperty("value") Object value) {}
}
