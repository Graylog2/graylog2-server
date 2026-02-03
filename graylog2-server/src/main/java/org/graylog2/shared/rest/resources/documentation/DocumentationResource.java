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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.Configuration;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.plugins.DocumentationRestResourceClasses;
import org.graylog2.shared.rest.documentation.generator.Generator;
import org.graylog2.shared.rest.documentation.openapi.OpenApiResource;
import org.graylog2.shared.rest.resources.RestResource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.initializers.JerseyService.PLUGIN_PREFIX;


/**
 * @deprecated Replaced by OpenAPI description generated with {@link OpenApiResource}.
 */
@Deprecated(forRemoval = true)
@Tag(name = "Documentation", description = "Documentation of this API in JSON format.")
@Path("/api-docs")
@RequiresAuthentication
public class DocumentationResource extends RestResource {

    private final Generator generator;
    private final HttpConfiguration httpConfiguration;

    @Inject
    public DocumentationResource(HttpConfiguration httpConfiguration,
                                 ObjectMapper objectMapper,
                                 DocumentationRestResourceClasses documentationRestResourceClasses,
                                 Configuration configuration) {

        this.httpConfiguration = requireNonNull(httpConfiguration, "httpConfiguration");

        final ImmutableSet.Builder<Class<?>> resourceClasses = ImmutableSet.<Class<?>>builder()
                .addAll(documentationRestResourceClasses.getSystemResources());

        // All plugin resources get the plugin prefix + the plugin package.
        final Map<Class<?>, String> pluginRestControllerMapping = new HashMap<>();
        for (Map.Entry<String, Set<Class<? extends PluginRestResource>>> entry : documentationRestResourceClasses.getPluginResourcesMap().entrySet()) {
            final String pluginPackage = entry.getKey();
            resourceClasses.addAll(entry.getValue());

            for (Class<? extends PluginRestResource> pluginRestResource : entry.getValue()) {
                pluginRestControllerMapping.put(pluginRestResource, pluginPackage);
            }
        }

        this.generator = new Generator(resourceClasses.build(), pluginRestControllerMapping, PLUGIN_PREFIX, objectMapper, configuration.isCloud(), true);
    }

    @GET
    @Timed
    @Operation(summary = "Get API documentation " +
            "- Deprecated: Consider the OpenAPI description at '/api/openapi.yaml' instead.", deprecated = true)
    @Produces(MediaType.APPLICATION_JSON)
    public Response overview() {
        return buildSuccessfulCORSResponse(generator.generateOverview());
    }

    @GET
    @Timed
    @Operation(summary = "Get API documentation with cluster global URI path " +
            "- Deprecated: Consider the OpenAPI description at '/api/openapi.yaml' instead.", deprecated = true)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/global")
    public Response globalOverview() {
        return buildSuccessfulCORSResponse(generator.generateOverview());
    }

    @GET
    @Timed
    @Operation(summary = "Get detailed API documentation of a single resource " +
            "- Deprecated: Consider the OpenAPI description at '/api/openapi.yaml' instead.", deprecated = true)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{route: .+}")
    public Response route(@Parameter(name = "route", description = "Route to fetch. For example /system", required = true)
                          @PathParam("route") String route,
                          @Context HttpHeaders httpHeaders) {
        // If the documentation was requested from "cluster global mode", use the HttpExternalUri for the baseUri.
        // Otherwise use the per node HttpPublishUri.
        URI baseUri;
        if (route.startsWith("global")) {
            route = route.replace("global", "");
            baseUri = RestTools.buildRelativeExternalUri(httpHeaders.getRequestHeaders(), httpConfiguration.getHttpExternalUri()).resolve(HttpConfiguration.PATH_API);
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
                // Indicate deprecation in accordance with https://datatracker.ietf.org/doc/rfc9745/.
                // RFC 9745 date format: @<unix-timestamp> for May 4, 2026 00:00:00 UTC (Graylog 7.1.0 release date)
                .header("Deprecation", "@1777852800")
                .build();
    }
}
