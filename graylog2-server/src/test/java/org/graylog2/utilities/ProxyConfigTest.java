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
package org.graylog2.utilities;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyConfigTest {

    @Test
    public void fromReturnsEmptyWithoutAConfiguredProxy() {
        assertThat(ProxyConfig.from(null)).isEmpty();
    }

    @Test
    public void fromWrapsAConfiguredProxy() {
        assertThat(ProxyConfig.from(URI.create("http://proxy.example.com:8123"))).isPresent();
    }

    @Test
    public void hostPortAndSchemeReadThroughToTheUri() {
        final ProxyConfig config = new ProxyConfig(URI.create("http://proxy.example.com:8123"));

        assertThat(config.host()).isEqualTo("proxy.example.com");
        assertThat(config.port()).isEqualTo(8123);
        assertThat(config.scheme()).isEqualTo("http");
    }

    @Test
    public void endpointStripsUserInfo() {
        final ProxyConfig config = new ProxyConfig(URI.create("http://user:pass@proxy.example.com:8123"));

        assertThat(config.endpoint()).isEqualTo(URI.create("http://proxy.example.com:8123"));
    }

    @Test
    public void credentialsAreAbsentWithoutUserInfo() {
        final ProxyConfig config = new ProxyConfig(URI.create("http://proxy.example.com:8123"));

        assertThat(config.credentials()).isEmpty();
    }

    @Test
    public void credentialsAreAbsentWithoutAColonSeparator() {
        // User info present but with no ":" to split on -- the same shape OkHttpClientProvider's
        // original code silently produced no credentials for, preserved here.
        final ProxyConfig config = new ProxyConfig(URI.create("http://justauser@proxy.example.com:8123"));

        assertThat(config.credentials()).isEmpty();
    }

    @Test
    public void credentialsParseUsernameAndPassword() {
        final ProxyConfig config = new ProxyConfig(URI.create("http://someuser:somepass@proxy.example.com:8123"));

        assertThat(config.credentials()).contains(new ProxyConfig.Credentials("someuser", "somepass"));
    }

    @Test
    public void credentialsAllowAnEmptyUsername() {
        final ProxyConfig config = new ProxyConfig(URI.create("http://:somepass@proxy.example.com:8123"));

        assertThat(config.credentials()).contains(new ProxyConfig.Credentials("", "somepass"));
    }

    @Test
    public void credentialsAllowAnEmptyPassword() {
        final ProxyConfig config = new ProxyConfig(URI.create("http://someuser:@proxy.example.com:8123"));

        assertThat(config.credentials()).contains(new ProxyConfig.Credentials("someuser", ""));
    }

    @Test
    public void credentialsAllowBothEmpty() {
        final ProxyConfig config = new ProxyConfig(URI.create("http://:@proxy.example.com:8123"));

        assertThat(config.credentials()).contains(new ProxyConfig.Credentials("", ""));
    }
}
