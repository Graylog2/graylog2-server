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

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.oauth.McpOAuthService;
import org.graylog.mcp.oauth.McpOAuthUrls;
import org.graylog.mcp.oauth.OAuthClient;
import org.graylog.mcp.oauth.OAuthException;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthResourceTest {
    private static final String REQUESTED_REDIRECT_URI = "http://127.0.0.1:50000/callback";
    private static final OAuthClient CLIENT = new OAuthClient("id", "mcp-client", "Claude Code", null,
            List.of("http://127.0.0.1:33418/callback"), Instant.EPOCH, Instant.EPOCH);

    private final McpOAuthService oauthService = mock(McpOAuthService.class);
    private final HttpHeaders headers = mock(HttpHeaders.class);
    private OAuthResource resource;

    @BeforeEach
    void setUp() {
        final HttpConfiguration httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://graylog.example.com/"));
        when(headers.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
        final McpConfiguration config = mock(McpConfiguration.class);
        when(config.oauthAvailable()).thenReturn(true);
        final ClusterConfigService clusterConfigService = mock(ClusterConfigService.class);
        when(clusterConfigService.getOrDefault(eq(McpConfiguration.class), any())).thenReturn(config);
        resource = new OAuthResource(oauthService, new McpOAuthUrls(httpConfiguration), clusterConfigService);
    }

    @Test
    void redirectsToConsentPageWithValidatedParameters() {
        when(oauthService.validateAuthorizationRequest(CLIENT.clientId(), REQUESTED_REDIRECT_URI, "code", "challenge", "S256", "a b", null))
                .thenReturn(new McpOAuthService.AuthorizationRequest(CLIENT, REQUESTED_REDIRECT_URI, "challenge", "a b", null));

        final Response response = resource.authorize(CLIENT.clientId(), REQUESTED_REDIRECT_URI, "code", "challenge", "S256", "a b", null, headers);

        assertThat(response.getStatus()).isEqualTo(Response.Status.SEE_OTHER.getStatusCode());
        assertThat(response.getLocation()).hasToString("https://graylog.example.com/oauth-consent?client_id=mcp-client"
                + "&redirect_uri=http%3A%2F%2F127.0.0.1%3A50000%2Fcallback&response_type=code&code_challenge=challenge"
                + "&code_challenge_method=S256&state=a+b");
    }

    @Test
    void returnsDescriptionForNonRedirectableErrors() {
        final OAuthException error = OAuthException.of(OAuthException.INVALID_REQUEST, "redirect_uri is not registered for this client");
        when(oauthService.validateAuthorizationRequest(any(), any(), any(), any(), any(), any(), any())).thenThrow(error);
        when(oauthService.errorRedirect(eq(error), any(), any(), any(), anyString())).thenReturn(Optional.empty());

        final Response response = resource.authorize(CLIENT.clientId(), "https://evil.example.com/cb", "code", "challenge", "S256", null, null, headers);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity()).isEqualTo("Invalid authorization request: redirect_uri is not registered for this client");
    }
}
