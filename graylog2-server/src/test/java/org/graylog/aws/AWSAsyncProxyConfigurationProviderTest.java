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
package org.graylog.aws;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AWSAsyncProxyConfigurationProvider} proxy configuration creation.
 */
public class AWSAsyncProxyConfigurationProviderTest {

    @Test
    public void buildProxyConfigurationWithoutCredentials() {
        final URI proxyUri = URI.create("http://proxy.example.com:8080");

        final ProxyConfiguration proxyConfig = AWSAsyncProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
        assertThat(proxyConfig.scheme()).isEqualTo("http");
        assertThat(proxyConfig.username()).isNull();
        assertThat(proxyConfig.password()).isNull();
    }

    @Test
    public void buildProxyConfigurationWithCredentials() {
        final URI proxyUri = URI.create("http://user:pass@proxy.example.com:8080");

        final ProxyConfiguration proxyConfig = AWSAsyncProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.username()).isEqualTo("user");
        assertThat(proxyConfig.password()).isEqualTo("pass");
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
    }

    @Test
    public void buildProxyConfigurationWithUsernameOnly() {
        final URI proxyUri = URI.create("http://user@proxy.example.com:8080");

        final ProxyConfiguration proxyConfig = AWSAsyncProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
        assertThat(proxyConfig.username()).isNull();
        assertThat(proxyConfig.password()).isNull();
    }

    @Test
    public void buildProxyConfigurationWithHttpsScheme() {
        final URI proxyUri = URI.create("https://admin:secret@secure-proxy.example.com:443");

        final ProxyConfiguration proxyConfig = AWSAsyncProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.username()).isEqualTo("admin");
        assertThat(proxyConfig.password()).isEqualTo("secret");
        assertThat(proxyConfig.scheme()).isEqualTo("https");
        assertThat(proxyConfig.host()).isEqualTo("secure-proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(443);
    }

    @Test
    public void buildProxyConfigurationWithDefaultHttpPort() {
        final URI proxyUri = URI.create("http://user:pass@proxy.example.com");

        final ProxyConfiguration proxyConfig = AWSAsyncProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.username()).isEqualTo("user");
        assertThat(proxyConfig.password()).isEqualTo("pass");
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        // The Netty proxy configuration requires an explicit port; the scheme default is used when none is provided.
        assertThat(proxyConfig.port()).isEqualTo(80);
    }

    @Test
    public void buildProxyConfigurationWithDefaultHttpsPort() {
        final URI proxyUri = URI.create("https://secure-proxy.example.com");

        final ProxyConfiguration proxyConfig = AWSAsyncProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.host()).isEqualTo("secure-proxy.example.com");
        assertThat(proxyConfig.scheme()).isEqualTo("https");
        assertThat(proxyConfig.port()).isEqualTo(443);
    }
}
