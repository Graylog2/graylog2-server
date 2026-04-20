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
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AWSProxyConfigurationProvider} proxy configuration creation.
 */
public class AWSProxyConfigurationProviderTest {

    @Test
    public void buildProxyConfigurationWithoutCredentials() {
        URI proxyUri = URI.create("http://proxy.example.com:8080");

        ProxyConfiguration proxyConfig = AWSProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
        assertThat(proxyConfig.username()).isNull();
        assertThat(proxyConfig.password()).isNull();
    }

    @Test
    public void buildProxyConfigurationWithCredentials() {
        URI proxyUri = URI.create("http://user:pass@proxy.example.com:8080");

        ProxyConfiguration proxyConfig = AWSProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.username()).contains("user");
        assertThat(proxyConfig.password()).contains("pass");
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
    }

    @Test
    public void buildProxyConfigurationWithUsernameOnly() {
        URI proxyUri = URI.create("http://user@proxy.example.com:8080");

        ProxyConfiguration proxyConfig = AWSProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
        assertThat(proxyConfig.username()).isNull();
        assertThat(proxyConfig.password()).isNull();
    }

    @Test
    public void buildProxyConfigurationWithHttpsScheme() {
        URI proxyUri = URI.create("https://admin:secret@secure-proxy.example.com:443");

        ProxyConfiguration proxyConfig = AWSProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.username()).contains("admin");
        assertThat(proxyConfig.password()).contains("secret");
        assertThat(proxyConfig.host()).isEqualTo("secure-proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(443);
    }

    @Test
    public void buildProxyConfigurationWithEmptyCredentials() {
        URI proxyUri = URI.create("http://:@proxy.example.com:8080");

        ProxyConfiguration proxyConfig = AWSProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.username()).contains("");
        assertThat(proxyConfig.password()).contains("");
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(8080);
    }

    @Test
    public void buildProxyConfigurationWithDefaultPort() {
        URI proxyUri = URI.create("http://user:pass@proxy.example.com");

        ProxyConfiguration proxyConfig = AWSProxyConfigurationProvider.buildProxyConfiguration(proxyUri);

        assertThat(proxyConfig.username()).contains("user");
        assertThat(proxyConfig.password()).contains("pass");
        assertThat(proxyConfig.host()).isEqualTo("proxy.example.com");
        assertThat(proxyConfig.port()).isEqualTo(-1);
    }
}
