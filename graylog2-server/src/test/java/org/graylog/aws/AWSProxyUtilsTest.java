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
 * <http://www.mongodb.org/licensing/server-side-public-license>.
 */
package org.graylog.aws;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AWSProxyUtils}.
 */
public class AWSProxyUtilsTest {

    @Test
    public void buildProxyConfigurationWithoutCredentials() {
        // Given: A proxy URI without user credentials
        URI proxyUri = URI.create("http://proxy.example.com:8080");

        // When: Building proxy configuration
        ProxyConfiguration proxyConfig = AWSProxyUtils.buildProxyConfiguration(proxyUri);

        // Then: The proxy should be configured without credentials
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
        assertThat(proxyConfig.username()).isNull();
        assertThat(proxyConfig.password()).isNull();
    }

    @Test
    public void buildProxyConfigurationWithCredentials() {
        // Given: A proxy URI with username and password
        URI proxyUri = URI.create("http://user:pass@proxy.example.com:8080");

        // When: Building proxy configuration
        ProxyConfiguration proxyConfig = AWSProxyUtils.buildProxyConfiguration(proxyUri);

        // Then: Credentials should be extracted and proxy configured properly
        assertThat(proxyConfig.username()).contains("user");
        assertThat(proxyConfig.password()).contains("pass");
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
    }

    @Test
    public void buildProxyConfigurationWithUsernameOnly() {
        // Given: A proxy URI with only username (edge case)
        URI proxyUri = URI.create("http://user@proxy.example.com:8080");

        // When: Building proxy configuration
        ProxyConfiguration proxyConfig = AWSProxyUtils.buildProxyConfiguration(proxyUri);

        // Then: Only username should be set (no password)
        // Note: Since there's no colon, the split won't produce 2 elements,
        // so credentials won't be set
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
        assertThat(proxyConfig.username()).isNull();
        assertThat(proxyConfig.password()).isNull();
    }

    @Test
    public void buildProxyConfigurationWithHttpsScheme() {
        // Given: A proxy URI with HTTPS scheme and credentials
        URI proxyUri = URI.create("https://admin:secret@secure-proxy.example.com:443");

        // When: Building proxy configuration
        ProxyConfiguration proxyConfig = AWSProxyUtils.buildProxyConfiguration(proxyUri);

        // Then: Credentials should be extracted
        assertThat(proxyConfig.username()).contains("admin");
        assertThat(proxyConfig.password()).contains("secret");
        assertThat(proxyConfig.host()).isEqualTo("secure-proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(443);
    }

    @Test
    public void buildProxyConfigurationWithEmptyCredentials() {
        // Given: A proxy URI with empty user info (colon but no username/password)
        URI proxyUri = URI.create("http://:@proxy.example.com:8080");

        // When: Building proxy configuration
        ProxyConfiguration proxyConfig = AWSProxyUtils.buildProxyConfiguration(proxyUri);

        // Then: Empty credentials should be set
        assertThat(proxyConfig.username()).contains("");
        assertThat(proxyConfig.password()).contains("");
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
    }

    @Test
    public void buildProxyConfigurationWithDefaultPort() {
        // Given: A proxy URI with credentials but no explicit port
        URI proxyUri = URI.create("http://user:pass@proxy.example.com");

        // When: Building proxy configuration
        ProxyConfiguration proxyConfig = AWSProxyUtils.buildProxyConfiguration(proxyUri);

        // Then: Credentials should be extracted and port should be default
        assertThat(proxyConfig.username()).contains("user");
        assertThat(proxyConfig.password()).contains("pass");
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        // Default port is -1 when not specified
        assertThat(proxyConfig.port()).isEqualTo(-1);
    }
}
