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
package org.graylog.storage.elasticsearch6.jest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import io.searchbox.client.JestClient;
import io.searchbox.client.http.JestHttpClient;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class JestClientProviderTest {
    @Test
    public void getReturnsJestHttpClient() {
        final JestClientProvider provider = new JestClientProvider(
                Collections.singletonList(URI.create("http://127.0.0.1:9200")),
                Duration.seconds(5L),
                Duration.seconds(5L),
                Duration.seconds(5L),
                8,
                2,
                2,
                false,
                null,
                Duration.seconds(5L),
                "http",
                false,
                null,
                null,
                new ObjectMapper()
        );

        final JestClient jestClient = provider.get();
        assertThat(jestClient).isInstanceOf(JestHttpClient.class);
    }

    @Test
    public void preemptiveAuthWithoutTrailingSlash() {
        final JestClientProvider provider = new JestClientProvider(
                Collections.singletonList(URI.create("http://elastic:changeme@127.0.0.1:9200")),
                Duration.seconds(5L),
                Duration.seconds(5L),
                Duration.seconds(5L),
                8,
                2,
                2,
                false,
                null,
                Duration.seconds(5L),
                "http",
                false,
                null,
                null,
                new ObjectMapper()
        );

        final JestClient jestClient = provider.get();
        assertThat(jestClient).isInstanceOf(JestHttpClient.class);
    }

    @Test
    public void preemptiveAuthWithTrailingSlash() {
        final JestClientProvider provider = new JestClientProvider(
                Collections.singletonList(URI.create("http://elastic:changeme@127.0.0.1:9200/")),
                Duration.seconds(5L),
                Duration.seconds(5L),
                Duration.seconds(5L),
                8,
                2,
                2,
                false,
                null,
                Duration.seconds(5L),
                "http",
                false,
                null,
                null,
                new ObjectMapper()
        );

        final JestClient jestClient = provider.get();
        assertThat(jestClient).isInstanceOf(JestHttpClient.class);
    }
}
