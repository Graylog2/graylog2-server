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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.oauth.McpOAuthService;
import org.graylog.mcp.oauth.McpOAuthUrls;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpOAuthResponseFilterTest extends JerseyTest {
    @Path("/other")
    public static class OtherResource {
        @GET
        public String get() {
            return "other";
        }
    }

    @BeforeAll
    static void setUpInjector() {
        GuiceInjectorHolder.createInjector(List.of());
    }

    @Override
    protected Application configure() {
        forceSet(TestProperties.CONTAINER_PORT, "0");
        final HttpConfiguration httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://graylog.example.com/"));
        final McpConfiguration config = mock(McpConfiguration.class);
        when(config.oauthAvailable()).thenReturn(true);
        final ClusterConfigService clusterConfigService = mock(ClusterConfigService.class);
        when(clusterConfigService.getOrDefault(eq(McpConfiguration.class), any())).thenReturn(config);
        final McpOAuthUrls urls = new McpOAuthUrls(httpConfiguration);

        return new ResourceConfig(OtherResource.class, McpOAuthResponseFilter.class)
                .register(new OAuthResource(mock(McpOAuthService.class), urls, clusterConfigService))
                .register(new OAuthMetadataResource(clusterConfigService, urls))
                .register(new OAuthFallbackResource(clusterConfigService, urls))
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(clusterConfigService).to(ClusterConfigService.class);
                        bind(urls).to(McpOAuthUrls.class);
                    }
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"oauth/register", "oauth/token", "oauth/revoke", ".well-known/oauth-protected-resource",
            ".well-known/oauth-protected-resource/api/mcp", ".well-known/oauth-authorization-server", "register", "token"})
    void answersPreflightWithCorsHeaders(String path) {
        final Response response = target(path).request()
                .header("Origin", "https://vscode.dev")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type, mcp-protocol-version")
                .options();

        assertThat(response.getStatus()).isLessThan(300);
        assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("*");
        assertThat(response.getHeaderString("Access-Control-Allow-Headers")).contains("MCP-Protocol-Version");
    }

    @ParameterizedTest
    @ValueSource(strings = {"other"})
    void doesNotAddCorsHeadersToOtherResources(String path) {
        final Response response = target(path).request().header("Origin", "https://evil.example").options();

        assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isNull();
    }
}
