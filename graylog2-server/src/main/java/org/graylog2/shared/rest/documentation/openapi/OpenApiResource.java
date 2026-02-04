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
package org.graylog2.shared.rest.documentation.openapi;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.rest.MoreMediaTypes;

/**
 * Serves the OpenAPI specification in JSON or YAML format.
 * <p>
 * Supported URLs:
 * <ul>
 *   <li>{@code /api/openapi} - JSON format (default)</li>
 *   <li>{@code /api/openapi.json} - JSON format (via Jersey's media type mapping)</li>
 *   <li>{@code /api/openapi.yaml} - YAML format</li>
 * </ul>
 */
@RequiresAuthentication
@Path("/openapi{ext: (\\.yaml)?}")
public class OpenApiResource extends BaseOpenApiResource {
    @Context
    Application app;

    @Inject
    public OpenApiResource(OpenAPIContextFactory contextFactory) {
        // This will initialize the OpenAPI context and register it as the default context.
        // The BaseOpenApiResource by default re-uses this context, if present, instead of configuring a new one.
        contextFactory.getOrCreate(OpenApiContext.OPENAPI_CONTEXT_ID_DEFAULT);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.APPLICATION_YAML})
    @Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers,
                               @Context UriInfo uriInfo,
                               @PathParam("ext") String ext) throws Exception {
        final String type = ".yaml".equals(ext) ? "yaml" : "json";
        return super.getOpenApi(headers, null, app, uriInfo, type);
    }
}
