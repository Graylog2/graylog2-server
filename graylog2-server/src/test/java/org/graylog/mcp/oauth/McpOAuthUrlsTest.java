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
package org.graylog.mcp.oauth;

import jakarta.ws.rs.core.MultivaluedHashMap;
import org.graylog2.configuration.HttpConfiguration;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpOAuthUrlsTest {
    @Test
    void buildsUrlsWithoutPathPrefix() {
        final McpOAuthUrls.Urls urls = new McpOAuthUrls.Urls(URI.create("https://graylog.example.com/"));

        assertThat(urls.issuer()).isEqualTo("https://graylog.example.com");
        assertThat(urls.resource()).isEqualTo("https://graylog.example.com/api/mcp");
        assertThat(urls.authorizationEndpoint()).isEqualTo("https://graylog.example.com/oauth/authorize");
        assertThat(urls.consentPage()).isEqualTo("https://graylog.example.com/oauth-consent");
        assertThat(urls.protectedResourceMetadata()).isEqualTo("https://graylog.example.com/.well-known/oauth-protected-resource/api/mcp");
        assertThat(urls.authorizationServerMetadata()).isEqualTo("https://graylog.example.com/.well-known/oauth-authorization-server");
    }

    @Test
    void insertsWellKnownSegmentBeforePathPrefix() {
        final McpOAuthUrls.Urls urls = new McpOAuthUrls.Urls(URI.create("https://example.com:8443/graylog"));

        assertThat(urls.issuer()).isEqualTo("https://example.com:8443/graylog");
        assertThat(urls.resource()).isEqualTo("https://example.com:8443/graylog/api/mcp");
        assertThat(urls.tokenEndpoint()).isEqualTo("https://example.com:8443/graylog/oauth/token");
        assertThat(urls.protectedResourceMetadata()).isEqualTo("https://example.com:8443/.well-known/oauth-protected-resource/graylog/api/mcp");
        assertThat(urls.authorizationServerMetadata()).isEqualTo("https://example.com:8443/.well-known/oauth-authorization-server/graylog");
    }

    @Test
    void usesOverrideHeaderAndFallsBackForRelativeValues() {
        final HttpConfiguration httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("http://10.0.0.1:9000/"));
        final McpOAuthUrls mcpOAuthUrls = new McpOAuthUrls(httpConfiguration);

        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(HttpConfiguration.OVERRIDE_HEADER, "https://graylog.example.com/");
        assertThat(mcpOAuthUrls.forRequest(headers).issuer()).isEqualTo("https://graylog.example.com");

        headers.putSingle(HttpConfiguration.OVERRIDE_HEADER, "/relative/");
        assertThat(mcpOAuthUrls.forRequest(headers).issuer()).isEqualTo("http://10.0.0.1:9000");
    }

    @Test
    void recognizesMcpResource() {
        assertThat(McpOAuthUrls.isMcpResource("https://graylog.example.com/api/mcp")).isTrue();
        assertThat(McpOAuthUrls.isMcpResource("https://graylog.example.com/graylog/api/mcp/")).isTrue();
        assertThat(McpOAuthUrls.isMcpResource("https://graylog.example.com/api/system")).isFalse();
        assertThat(McpOAuthUrls.isMcpResource("https://graylog.example.com/api/mcp#x")).isFalse();
        assertThat(McpOAuthUrls.isMcpResource("ftp://graylog.example.com/api/mcp")).isFalse();
        assertThat(McpOAuthUrls.isMcpResource("not a uri")).isFalse();
    }
}
