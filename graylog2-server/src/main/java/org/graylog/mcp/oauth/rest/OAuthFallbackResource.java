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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.oauth.McpOAuthUrls;
import org.graylog.mcp.oauth.OAuthException;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.NonApiResource;
import org.graylog2.shared.rest.SkipCSRFProtection;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * MCP clients fall back to these root-level endpoints when OAuth metadata discovery fails. Answering with OAuth errors
 * lets them report the cause instead of failing to parse the web interface's HTML.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-03-26/basic/authorization#2-3-3-fallbacks-for-servers-without-metadata-discovery">MCP authorization fallbacks</a>
 */
@NonApiResource
// A non-empty class path keeps Jersey from merging this with other root resources, which would break their prefixes
@Path("{endpoint: authorize|register|token}")
public class OAuthFallbackResource {
    private final ClusterConfigService clusterConfigService;
    private final McpOAuthUrls urls;

    @Inject
    public OAuthFallbackResource(ClusterConfigService clusterConfigService, McpOAuthUrls urls) {
        this.clusterConfigService = clusterConfigService;
        this.urls = urls;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpHeaders headers) {
        return error(headers);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("OAuth requests are made by non-browser clients")
    @NoAuditEvent("Only returns an error")
    public Response post(@Context HttpHeaders headers) {
        return error(headers);
    }

    private Response error(HttpHeaders headers) {
        final String description = clusterConfigService.getOrDefault(McpConfiguration.class, McpConfiguration.DEFAULT_VALUES).oauthAvailable()
                ? f("OAuth endpoints are advertised at %s", urls.forRequest(headers.getRequestHeaders()).authorizationServerMetadata())
                : "MCP OAuth is not enabled. Enable remote access and OAuth in the MCP configuration.";
        return OAuthResource.error(OAuthException.of(OAuthException.INVALID_REQUEST, description, Response.Status.NOT_FOUND));
    }
}
