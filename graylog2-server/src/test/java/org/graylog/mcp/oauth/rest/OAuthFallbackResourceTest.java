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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.oauth.McpOAuthUrls;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthFallbackResourceTest {
    private final ClusterConfigService clusterConfigService = mock(ClusterConfigService.class);
    private final HttpHeaders headers = mock(HttpHeaders.class);
    private OAuthFallbackResource resource;

    @BeforeEach
    void setUp() {
        final HttpConfiguration httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://graylog.example.com/"));
        when(headers.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
        resource = new OAuthFallbackResource(clusterConfigService, new McpOAuthUrls(httpConfiguration));
    }

    @Test
    void returnsJsonErrorWhenOAuthIsDisabled() {
        mockConfig(false);

        final Response response = resource.post(headers);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.getEntity()).isInstanceOfSatisfying(OAuthResource.ErrorResponse.class, error -> {
            assertThat(error.error()).isEqualTo("invalid_request");
            assertThat(error.errorDescription()).startsWith("MCP OAuth is not enabled.");
        });
    }

    @Test
    void pointsToMetadataWhenOAuthIsEnabled() {
        mockConfig(true);

        final Response response = resource.get(headers);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getEntity()).isInstanceOfSatisfying(OAuthResource.ErrorResponse.class, error ->
                assertThat(error.errorDescription()).isEqualTo("OAuth endpoints are advertised at https://graylog.example.com/.well-known/oauth-authorization-server"));
    }

    private void mockConfig(boolean oauthAvailable) {
        final McpConfiguration config = mock(McpConfiguration.class);
        when(config.oauthAvailable()).thenReturn(oauthAvailable);
        when(clusterConfigService.getOrDefault(eq(McpConfiguration.class), any())).thenReturn(config);
    }
}
