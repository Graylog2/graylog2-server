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
package org.graylog.mcp.oauth.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.oauth.McpOAuthUrls;
import org.graylog.mcp.server.McpRestResource;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Points unauthenticated MCP clients to the OAuth discovery endpoint (RFC 9728 section 5.1) and allows browser-based
 * MCP clients to call the OAuth endpoints.
 */
public class McpOAuthResponseFilter implements ContainerResponseFilter {
    private static final Set<Class<?>> CORS_RESOURCES = Set.of(OAuthResource.class, OAuthMetadataResource.class);

    private final ResourceInfo resourceInfo;
    private final ClusterConfigService clusterConfigService;
    private final McpOAuthUrls urls;

    @Inject
    public McpOAuthResponseFilter(@Context ResourceInfo resourceInfo, ClusterConfigService clusterConfigService, McpOAuthUrls urls) {
        this.resourceInfo = resourceInfo;
        this.clusterConfigService = clusterConfigService;
        this.urls = urls;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        if (resourceClass == null) {
            return;
        }
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        if (CORS_RESOURCES.contains(resourceClass)) {
            // These endpoints don't use cookies, so allowing any origin is safe
            headers.putSingle("Access-Control-Allow-Origin", "*");
            headers.putSingle("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.putSingle("Access-Control-Allow-Headers", "Content-Type, Authorization, MCP-Protocol-Version");
            headers.putSingle("Access-Control-Max-Age", "86400");
            return;
        }
        if (McpRestResource.class.isAssignableFrom(resourceClass)
                && responseContext.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
                && clusterConfigService.getOrDefault(McpConfiguration.class, McpConfiguration.DEFAULT_VALUES).oauthAvailable()) {
            final String resourceMetadata = urls.forRequest(requestContext.getHeaders()).protectedResourceMetadata();
            final String authorization = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            final String error = authorization != null && authorization.startsWith("Bearer ") ? "error=\"invalid_token\", " : "";
            headers.putSingle(HttpHeaders.WWW_AUTHENTICATE, f("Bearer %sresource_metadata=\"%s\"", error, resourceMetadata));
        }
    }
}
