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
package org.graylog2.shared.rest.resources.documentation;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.inject.RestControllerPackage;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.plugins.PluginRestResourceClasses;
import org.graylog2.shared.rest.documentation.generator.Generator;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.initializers.JerseyService.PLUGIN_PREFIX;

@Api(value = "Documentation", description = "Documentation of this API in JSON format.")
@Path("/api-docs")
public class DocumentationResource extends RestResource {

    private final Generator generator;
    private final HttpConfiguration httpConfiguration;

    @Inject
    public DocumentationResource(HttpConfiguration httpConfiguration,
                                 Set<RestControllerPackage> restControllerPackages,
                                 ObjectMapper objectMapper,
                                 PluginRestResourceClasses pluginRestResourceClasses) {

        this.httpConfiguration = requireNonNull(httpConfiguration, "httpConfiguration");

        final ImmutableSet.Builder<String> packageNames = ImmutableSet.<String>builder()
                .addAll(restControllerPackages.stream()
                        .map(RestControllerPackage::name)
                        .collect(Collectors.toList()));

        // All plugin resources get the plugin prefix + the plugin package.
        final Map<Class<?>, String> pluginRestControllerMapping = new HashMap<>();
        for (Map.Entry<String, Set<Class<? extends PluginRestResource>>> entry : pluginRestResourceClasses.getMap().entrySet()) {
            final String pluginPackage = entry.getKey();
            packageNames.add(pluginPackage);

            for (Class<? extends PluginRestResource> pluginRestResource : entry.getValue()) {
                pluginRestControllerMapping.put(pluginRestResource, pluginPackage);
            }
        }

        this.generator = new Generator(packageNames.build(), pluginRestControllerMapping, PLUGIN_PREFIX, objectMapper);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get API documentation")
    @Produces(MediaType.APPLICATION_JSON)
    public Response overview() {
        return buildSuccessfulCORSResponse(generator.generateOverview());
    }

    @GET
    @Timed
    @ApiOperation(value = "Get API documentation with cluster global URI path")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/global")
    public Response globalOverview() {
        return buildSuccessfulCORSResponse(generator.generateOverview());
    }

    @GET
    @Timed
    @ApiOperation(value = "Get detailed API documentation of a single resource")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{route: .+}")
    public Response route(@ApiParam(name = "route", value = "Route to fetch. For example /system", required = true)
                          @PathParam("route") String route,
                          @Context HttpHeaders httpHeaders) {
        // If the documentation was requested from "cluster global mode", use the HttpExternalUri for the baseUri.
        // Otherwise use the per node HttpPublishUri.
        URI baseUri;
        if (route.startsWith("global")) {
            route = route.replace("global", "");
            baseUri = RestTools.buildExternalUri(httpHeaders.getRequestHeaders(), httpConfiguration.getHttpExternalUri()).resolve(HttpConfiguration.PATH_API);
        } else {
            baseUri = httpConfiguration.getHttpPublishUri().resolve(HttpConfiguration.PATH_API);
        }
        return buildSuccessfulCORSResponse(generator.generateForRoute(route, baseUri.toString()));
    }

    private Response buildSuccessfulCORSResponse(Map<String, Object> result) {
        return Response.ok(result)
                .header("Access-Control-Allow-Origin", "*") // Headers for Swagger UI.
                .header("Access-Control-Allow-Methods", "GET")
                .header("Access-Control-Allow-Headers", "Content-Type, api_key, Authorization")
                .build();
    }
}
