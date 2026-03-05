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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.RestTools;

import java.net.URI;

/**
 * Serves the OpenAPI specification in JSON or YAML format.
 * <p>
 * The {@code servers} field is set dynamically per request based on the
 * {@code X-Graylog-Server-URL} header, falling back to {@code http_external_uri}
 * or {@code http_publish_uri} configuration.
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
public class OpenApiResource {

    private final OpenApiContext context;
    private final HttpConfiguration httpConfiguration;

    @Inject
    public OpenApiResource(OpenAPIContextFactory contextFactory, HttpConfiguration httpConfiguration) {
        this.context = contextFactory.getOrCreate(OpenApiContext.OPENAPI_CONTEXT_ID_DEFAULT);
        this.httpConfiguration = httpConfiguration;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.APPLICATION_YAML})
    @Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers,
                               @Context UriInfo uriInfo,
                               @PathParam("ext") String ext) throws Exception {
        final boolean yaml = ".yaml".equals(ext);
        final var mapper = yaml ? context.getOutputYamlMapper() : context.getOutputJsonMapper();
        final var mediaType = yaml ? MoreMediaTypes.APPLICATION_YAML : MediaType.APPLICATION_JSON;

        final OpenAPI openAPI = context.read();
        final String output = serializeWithServerUrl(openAPI, mapper, resolveServerUrl(headers));

        return Response.ok(output).type(mediaType).build();
    }

    private URI resolveServerUrl(HttpHeaders headers) {
        final var baseUri = RestTools.buildExternalUri(
                headers.getRequestHeaders(), httpConfiguration.getHttpExternalUri());
        return baseUri.resolve(HttpConfiguration.PATH_API);
    }

    private String serializeWithServerUrl(OpenAPI openAPI, ObjectMapper mapper, URI serverUrl)
            throws JsonProcessingException {
        final var tree = mapper.valueToTree(openAPI);
        if (tree instanceof ObjectNode root) {
            final var serversArray = mapper.createArrayNode();
            final var serverNode = mapper.createObjectNode();
            serverNode.put("url", serverUrl.toString());
            serversArray.add(serverNode);
            root.set("servers", serversArray);
        }
        return mapper.writeValueAsString(tree);
    }
}
