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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.oauth.McpOAuthService;
import org.graylog.mcp.oauth.McpOAuthUrls;
import org.graylog.mcp.oauth.OAuthException;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Endpoints used by the web interface to approve MCP OAuth authorization requests and to manage connected clients.
 */
@RequiresAuthentication
@Path("/mcp/oauth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "MCP/OAuth", description = "Authorization of MCP clients via OAuth")
public class McpOAuthResource extends RestResource {
    private final McpOAuthService oauthService;
    private final McpOAuthUrls urls;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public McpOAuthResource(McpOAuthService oauthService, McpOAuthUrls urls, ClusterConfigService clusterConfigService) {
        this.oauthService = oauthService;
        this.urls = urls;
        this.clusterConfigService = clusterConfigService;
    }

    public record AuthorizationRequestDetails(@JsonProperty("client_id") String clientId,
                                              @JsonProperty("client_name") String clientName,
                                              @JsonProperty("client_uri") @Nullable String clientUri,
                                              @JsonProperty("redirect_uri") String redirectUri,
                                              @JsonProperty("redirect_host") String redirectHost,
                                              @JsonProperty("client_registered_at") Instant clientRegisteredAt) {}

    public record AuthorizationDecision(@JsonProperty("client_id") @NotNull String clientId,
                                        @JsonProperty("redirect_uri") @Nullable String redirectUri,
                                        @JsonProperty("response_type") @Nullable String responseType,
                                        @JsonProperty("code_challenge") @Nullable String codeChallenge,
                                        @JsonProperty("code_challenge_method") @Nullable String codeChallengeMethod,
                                        @JsonProperty("state") @Nullable String state,
                                        @JsonProperty("resource") @Nullable String resource,
                                        @JsonProperty("approved") boolean approved) {}

    public record AuthorizationDecisionResponse(@JsonProperty("redirect_uri") String redirectUri) {}

    public record GrantSummary(@JsonProperty("client_id") String clientId,
                               @JsonProperty("client_name") String clientName,
                               @JsonProperty("last_refreshed_at") Instant lastRefreshedAt,
                               @JsonProperty("expires_at") Instant expiresAt) {}

    public record GrantList(@JsonProperty("grants") List<GrantSummary> grants) {}

    @GET
    @Path("authorization-request")
    @Operation(summary = "Validates an MCP OAuth authorization request and returns details for the consent page")
    public AuthorizationRequestDetails authorizationRequest(@QueryParam("client_id") String clientId,
                                                            @QueryParam("redirect_uri") String redirectUri,
                                                            @QueryParam("response_type") String responseType,
                                                            @QueryParam("code_challenge") String codeChallenge,
                                                            @QueryParam("code_challenge_method") String codeChallengeMethod,
                                                            @QueryParam("state") String state,
                                                            @QueryParam("resource") String resource) {
        ensureAllowed();
        final McpOAuthService.AuthorizationRequest request = validate(clientId, redirectUri, responseType,
                codeChallenge, codeChallengeMethod, state, resource);
        return new AuthorizationRequestDetails(
                request.client().clientId(),
                request.client().clientName(),
                request.client().clientUri(),
                request.redirectUri(),
                redirectHost(request.redirectUri()),
                request.client().createdAt()
        );
    }

    @POST
    @Path("authorization-request")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Approves or denies an MCP OAuth authorization request")
    @NoAuditEvent("Has custom audit events")
    public AuthorizationDecisionResponse decide(@NotNull AuthorizationDecision decision, @Context HttpHeaders headers) {
        ensureAllowed();
        final McpOAuthService.AuthorizationRequest request = validate(decision.clientId(), decision.redirectUri(),
                decision.responseType(), decision.codeChallenge(), decision.codeChallengeMethod(), decision.state(),
                decision.resource());
        final String issuer = urls.forRequest(headers.getRequestHeaders()).issuer();
        final String redirect = decision.approved()
                ? oauthService.approve(request, getCurrentUser(), issuer)
                : oauthService.deny(request, getCurrentUser(), issuer);
        return new AuthorizationDecisionResponse(redirect);
    }

    @GET
    @Path("grants")
    @Operation(summary = "Lists the MCP clients the current user has authorized")
    public GrantList grants() {
        return new GrantList(oauthService.grantsForUser(getCurrentUser().getName()).stream()
                .map(grant -> new GrantSummary(grant.clientId(), grant.clientName(), grant.lastRefreshedAt(), grant.expiresAt()))
                .toList());
    }

    @DELETE
    @Path("grants/{clientId}")
    @Operation(summary = "Revokes the authorization of an MCP client for the current user")
    @NoAuditEvent("Has custom audit events")
    public void revokeGrant(@Parameter(name = "clientId", required = true) @PathParam("clientId") String clientId) {
        if (!oauthService.revokeGrant(getCurrentUser(), clientId)) {
            throw new NotFoundException("No authorization found for this client");
        }
    }

    private void ensureAllowed() {
        final McpConfiguration config = clusterConfigService.getOrDefault(McpConfiguration.class, McpConfiguration.DEFAULT_VALUES);
        if (!config.oauthAvailable()) {
            throw new NotFoundException("MCP OAuth is not enabled");
        }
        if (!isPermitted(RestPermissions.MCP_SERVER_ACCESS)) {
            throw new ForbiddenException("You are not allowed to access the MCP server");
        }
    }

    private McpOAuthService.AuthorizationRequest validate(String clientId, String redirectUri, String responseType,
                                                          String codeChallenge, String codeChallengeMethod,
                                                          String state, String resource) {
        try {
            return oauthService.validateAuthorizationRequest(clientId, redirectUri, responseType, codeChallenge,
                    codeChallengeMethod, state, resource);
        } catch (OAuthException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private static String redirectHost(String redirectUri) {
        final URI uri = URI.create(redirectUri);
        return uri.getHost() != null ? uri.getHost() : uri.getScheme();
    }
}
