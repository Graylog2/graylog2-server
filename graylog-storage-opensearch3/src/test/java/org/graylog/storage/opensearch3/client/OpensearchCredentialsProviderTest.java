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
package org.graylog.storage.opensearch3.client;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

class OpensearchCredentialsProviderTest {

    @Test
    void testHostCredentialsParsing() {
        final List<URI> hosts = List.of(
                URI.create("http://my-user:secret-password@my-opensearch-server"),
                URI.create("http://max:asdfgh@second-node")
        );
        final CredentialsProvider provider = new OpensearchCredentialsProvider(hosts, "foo", "bar", false).get();

        Assertions.assertThat(credentialsForHost(provider, "my-opensearch-server"))
                .isInstanceOf(UsernamePasswordCredentials.class)
                .isEqualTo(new UsernamePasswordCredentials("my-user", "my-password".toCharArray()));

        Assertions.assertThat(credentialsForHost(provider, "second-node"))
                .isInstanceOf(UsernamePasswordCredentials.class)
                .isEqualTo(new UsernamePasswordCredentials("max", "asdfgh".toCharArray()));
    }

    @Test
    void testDefaultCredentials() {
        final List<URI> hosts = List.of(
                URI.create("http://my-opensearch-server"),
                URI.create("http://second-node")
        );
        final CredentialsProvider provider = new OpensearchCredentialsProvider(hosts, "foo", "bar", true).get();

        Assertions.assertThat(credentialsForHost(provider, "my-opensearch-server"))
                .isInstanceOf(UsernamePasswordCredentials.class)
                .isEqualTo(new UsernamePasswordCredentials("foo", "bar".toCharArray()));
    }

    private static Credentials credentialsForHost(CredentialsProvider provider, String host) {
        return provider.getCredentials(new AuthScope(host, -1), new BasicHttpContext());
    }
}
