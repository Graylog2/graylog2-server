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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.oauth.McpOAuthService;
import org.graylog.mcp.oauth.McpOAuthUrls;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.NonApiResource;

import java.util.List;

/**
 * OAuth discovery endpoints used by MCP clients.
 * <p>
 * The variants with a path suffix are required when Graylog is served below a path prefix, see RFC 9728 section 3.1
 * and RFC 8414 section 3.1.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9728">RFC 9728 - OAuth 2.0 Protected Resource Metadata</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8414">RFC 8414 - OAuth 2.0 Authorization Server Metadata</a>
 */
@NonApiResource(prefix = "/.well-known")
@Path("")
public class OAuthMetadataResource {
    private final ClusterConfigService clusterConfigService;
    private final McpOAuthUrls urls;

    @Inject
    public OAuthMetadataResource(ClusterConfigService clusterConfigService, McpOAuthUrls urls) {
        this.clusterConfigService = clusterConfigService;
        this.urls = urls;
    }

    public record ProtectedResourceMetadata(@JsonProperty("resource") String resource,
                                            @JsonProperty("authorization_servers") List<String> authorizationServers,
                                            @JsonProperty("bearer_methods_supported") List<String> bearerMethodsSupported,
                                            @JsonProperty("resource_name") String resourceName) {}

    public record AuthorizationServerMetadata(@JsonProperty("issuer") String issuer,
                                              @JsonProperty("authorization_endpoint") String authorizationEndpoint,
                                              @JsonProperty("token_endpoint") String tokenEndpoint,
                                              @JsonProperty("registration_endpoint") String registrationEndpoint,
                                              @JsonProperty("revocation_endpoint") String revocationEndpoint,
                                              @JsonProperty("response_types_supported") List<String> responseTypesSupported,
                                              @JsonProperty("grant_types_supported") List<String> grantTypesSupported,
                                              @JsonProperty("code_challenge_methods_supported") List<String> codeChallengeMethodsSupported,
                                              @JsonProperty("token_endpoint_auth_methods_supported") List<String> tokenEndpointAuthMethodsSupported,
                                              @JsonProperty("revocation_endpoint_auth_methods_supported") List<String> revocationEndpointAuthMethodsSupported,
                                              @JsonProperty("authorization_response_iss_parameter_supported") boolean authorizationResponseIssParameterSupported) {}

    @GET
    @Path("oauth-protected-resource")
    @Produces(MediaType.APPLICATION_JSON)
    public ProtectedResourceMetadata protectedResource(@Context HttpHeaders headers) {
        ensureAvailable();
        final McpOAuthUrls.Urls requestUrls = urls.forRequest(headers.getRequestHeaders());
        return new ProtectedResourceMetadata(
                requestUrls.resource(),
                List.of(requestUrls.issuer()),
                List.of("header"),
                "Graylog MCP Server"
        );
    }

    @GET
    @Path("oauth-protected-resource/{suffix: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProtectedResourceMetadata protectedResourceWithSuffix(@Context HttpHeaders headers) {
        return protectedResource(headers);
    }

    @GET
    @Path("oauth-authorization-server")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthorizationServerMetadata authorizationServer(@Context HttpHeaders headers) {
        ensureAvailable();
        final McpOAuthUrls.Urls requestUrls = urls.forRequest(headers.getRequestHeaders());
        return new AuthorizationServerMetadata(
                requestUrls.issuer(),
                requestUrls.authorizationEndpoint(),
                requestUrls.tokenEndpoint(),
                requestUrls.registrationEndpoint(),
                requestUrls.revocationEndpoint(),
                List.of(McpOAuthService.RESPONSE_TYPE_CODE),
                List.of(McpOAuthService.GRANT_TYPE_AUTHORIZATION_CODE, McpOAuthService.GRANT_TYPE_REFRESH_TOKEN),
                List.of(McpOAuthService.CODE_CHALLENGE_METHOD_S256),
                List.of(McpOAuthService.TOKEN_ENDPOINT_AUTH_METHOD_NONE),
                List.of(McpOAuthService.TOKEN_ENDPOINT_AUTH_METHOD_NONE),
                true
        );
    }

    @GET
    @Path("oauth-authorization-server/{suffix: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthorizationServerMetadata authorizationServerWithSuffix(@Context HttpHeaders headers) {
        return authorizationServer(headers);
    }

    private void ensureAvailable() {
        if (!clusterConfigService.getOrDefault(McpConfiguration.class, McpConfiguration.DEFAULT_VALUES).oauthAvailable()) {
            throw new NotFoundException();
        }
    }
}
