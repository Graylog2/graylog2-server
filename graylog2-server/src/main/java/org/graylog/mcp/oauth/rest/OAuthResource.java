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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.oauth.McpOAuthService;
import org.graylog.mcp.oauth.McpOAuthUrls;
import org.graylog.mcp.oauth.OAuthClient;
import org.graylog.mcp.oauth.OAuthException;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.NonApiResource;
import org.graylog2.shared.rest.SkipCSRFProtection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Unauthenticated OAuth 2.1 endpoints for MCP clients. The consent step happens in the web interface.
 */
@NonApiResource(prefix = "/oauth")
@Path("")
public class OAuthResource {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthResource.class);

    private final McpOAuthService oauthService;
    private final McpOAuthUrls urls;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public OAuthResource(McpOAuthService oauthService, McpOAuthUrls urls, ClusterConfigService clusterConfigService) {
        this.oauthService = oauthService;
        this.urls = urls;
        this.clusterConfigService = clusterConfigService;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegistrationRequest(@JsonProperty("redirect_uris") @Nullable List<String> redirectUris,
                                      @JsonProperty("client_name") @Nullable String clientName,
                                      @JsonProperty("client_uri") @Nullable String clientUri,
                                      @JsonProperty("grant_types") @Nullable List<String> grantTypes,
                                      @JsonProperty("response_types") @Nullable List<String> responseTypes) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RegistrationResponse(@JsonProperty("client_id") String clientId,
                                       @JsonProperty("client_id_issued_at") long clientIdIssuedAt,
                                       @JsonProperty("client_name") String clientName,
                                       @JsonProperty("client_uri") @Nullable String clientUri,
                                       @JsonProperty("redirect_uris") List<String> redirectUris,
                                       @JsonProperty("grant_types") List<String> grantTypes,
                                       @JsonProperty("response_types") List<String> responseTypes,
                                       @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod) {}

    public record TokenResponse(@JsonProperty("access_token") String accessToken,
                                @JsonProperty("token_type") String tokenType,
                                @JsonProperty("expires_in") long expiresIn,
                                @JsonProperty("refresh_token") String refreshToken) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(@JsonProperty("error") String error,
                                @JsonProperty("error_description") @Nullable String errorDescription) {}

    @GET
    @Path("authorize")
    public Response authorize(@QueryParam("client_id") String clientId,
                              @QueryParam("redirect_uri") String redirectUri,
                              @QueryParam("response_type") String responseType,
                              @QueryParam("code_challenge") String codeChallenge,
                              @QueryParam("code_challenge_method") String codeChallengeMethod,
                              @QueryParam("state") String state,
                              @QueryParam("resource") String resource,
                              @Context HttpHeaders headers,
                              @Context UriInfo uriInfo) {
        if (!oauthAvailable()) {
            return plainError(Response.Status.NOT_FOUND, "MCP OAuth is not enabled.");
        }
        final McpOAuthUrls.Urls requestUrls = urls.forRequest(headers.getRequestHeaders());
        try {
            oauthService.validateAuthorizationRequest(clientId, redirectUri, responseType, codeChallenge, codeChallengeMethod, state, resource);
        } catch (OAuthException e) {
            final Optional<String> errorRedirect = oauthService.errorRedirect(e, clientId, redirectUri, state, requestUrls.issuer());
            if (errorRedirect.isPresent()) {
                return Response.seeOther(URI.create(errorRedirect.get())).build();
            }
            LOG.debug("Rejecting OAuth authorization request of client <{}>: {}", clientId, e.getMessage());
            return plainError(Response.Status.BAD_REQUEST, f("Invalid authorization request: %s", e.getMessage()));
        }
        // The consent page validates the request again, so passing the raw query is fine
        return Response.seeOther(URI.create(f("%s?%s", requestUrls.consentPage(), uriInfo.getRequestUri().getRawQuery()))).build();
    }

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("OAuth dynamic client registration is used by non-browser clients")
    @NoAuditEvent("Has custom audit events")
    public Response register(@Nullable RegistrationRequest request) {
        if (!oauthAvailable()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (request == null) {
            return error(OAuthException.of(OAuthException.INVALID_CLIENT_METADATA, "Missing client metadata"));
        }
        try {
            final OAuthClient client = oauthService.registerClient(new McpOAuthService.ClientRegistrationRequest(
                    request.redirectUris(),
                    request.clientName(),
                    request.clientUri(),
                    request.grantTypes(),
                    request.responseTypes()
            ), config());
            return Response.status(Response.Status.CREATED)
                    .cacheControl(noStore())
                    .entity(new RegistrationResponse(
                            client.clientId(),
                            client.createdAt().getEpochSecond(),
                            client.clientName(),
                            client.clientUri(),
                            client.redirectUris(),
                            List.of(McpOAuthService.GRANT_TYPE_AUTHORIZATION_CODE, McpOAuthService.GRANT_TYPE_REFRESH_TOKEN),
                            List.of(McpOAuthService.RESPONSE_TYPE_CODE),
                            McpOAuthService.TOKEN_ENDPOINT_AUTH_METHOD_NONE
                    ))
                    .build();
        } catch (OAuthException e) {
            return error(e);
        }
    }

    @POST
    @Path("token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("OAuth token requests are made by non-browser clients")
    @NoAuditEvent("Has custom audit events")
    public Response token(@FormParam("grant_type") String grantType,
                          @FormParam("client_id") String clientId,
                          @FormParam("code") String code,
                          @FormParam("redirect_uri") String redirectUri,
                          @FormParam("code_verifier") String codeVerifier,
                          @FormParam("refresh_token") String refreshToken,
                          @FormParam("resource") String resource) {
        if (!oauthAvailable()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            final McpOAuthService.TokenResponse tokens = switch (grantType == null ? "" : grantType) {
                case McpOAuthService.GRANT_TYPE_AUTHORIZATION_CODE ->
                        oauthService.exchangeAuthorizationCode(clientId, code, redirectUri, codeVerifier, resource, config());
                case McpOAuthService.GRANT_TYPE_REFRESH_TOKEN -> oauthService.refresh(clientId, refreshToken, resource, config());
                default -> throw OAuthException.of(OAuthException.UNSUPPORTED_GRANT_TYPE, "Unsupported grant_type");
            };
            return Response.ok(new TokenResponse(tokens.accessToken(), "Bearer", tokens.expiresIn(), tokens.refreshToken()))
                    .cacheControl(noStore())
                    .header("Pragma", "no-cache")
                    .build();
        } catch (OAuthException e) {
            LOG.debug("OAuth token request of client <{}> failed: {} ({})", clientId, e.error(), e.getMessage());
            return error(e);
        }
    }

    @POST
    @Path("revoke")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @SkipCSRFProtection("OAuth token revocation requests are made by non-browser clients")
    @NoAuditEvent("Has custom audit events")
    public Response revoke(@FormParam("client_id") String clientId, @FormParam("token") String token) {
        if (!oauthAvailable()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            oauthService.revoke(clientId, token);
            return Response.ok().build();
        } catch (OAuthException e) {
            return error(e);
        }
    }

    @OPTIONS
    @Path("{endpoint: register|token|revoke}")
    public Response preflight() {
        return Response.noContent().build();
    }

    private boolean oauthAvailable() {
        return config().oauthAvailable();
    }

    private McpConfiguration config() {
        return clusterConfigService.getOrDefault(McpConfiguration.class, McpConfiguration.DEFAULT_VALUES);
    }

    private static Response error(OAuthException e) {
        return Response.status(e.status())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .cacheControl(noStore())
                .entity(new ErrorResponse(e.error(), e.description()))
                .build();
    }

    private static Response plainError(Response.Status status, String message) {
        return Response.status(status).type(MediaType.TEXT_PLAIN_TYPE).entity(message).build();
    }

    private static CacheControl noStore() {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoStore(true);
        return cacheControl;
    }
}
