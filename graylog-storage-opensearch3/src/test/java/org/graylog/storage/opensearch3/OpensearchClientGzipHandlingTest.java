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
package org.graylog.storage.opensearch3;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.ImmutableList;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okio.Buffer;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.InfoResponse;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the division of labor for response gzip handling between opensearch-java and Apache HttpClient.
 *
 * <p>A gzip-encoded response must be decompressed by <em>exactly one</em> layer:
 * <ul>
 *   <li>opensearch-java &lt; 3.6.0 decompresses unconditionally itself, so HttpClient's transparent
 *       decompression (on by default since HttpClient 5.6) must be switched off via
 *       {@code disableContentCompression()} in {@link OfficialOpensearchClientProvider} — otherwise the
 *       body is inflated twice ("Not in GZIP format").</li>
 *   <li>opensearch-java &gt;= 3.6.0 no longer decompresses
 *       (see https://github.com/opensearch-project/opensearch-java/pull/1844) and relies on HttpClient,
 *       so {@code disableContentCompression()} must be removed — otherwise nobody inflates the body.</li>
 * </ul>
 *
 * <p>This test fails in either misconfiguration. If it starts failing after an opensearch-java upgrade,
 * remove {@code disableContentCompression()} from {@code OfficialOpensearchClientProvider}.
 */
class OpensearchClientGzipHandlingTest {

    // A minimal but complete OpenSearch info() response so the client call parses cleanly.
    private static final String INFO_JSON = """
            {
              "name": "test-node",
              "cluster_name": "test-cluster",
              "cluster_uuid": "test-uuid",
              "version": {
                "distribution": "opensearch",
                "number": "2.11.0",
                "build_type": "tar",
                "build_hash": "deadbeef",
                "build_date": "2023-01-01T00:00:00.000Z",
                "build_snapshot": false,
                "lucene_version": "9.7.0",
                "minimum_wire_compatibility_version": "7.10.0",
                "minimum_index_compatibility_version": "7.0.0"
              },
              "tagline": "The OpenSearch Project"
            }""";

    private final MockWebServer server = new MockWebServer();

    @BeforeEach
    void setUp() throws Exception {
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.close();
    }

    @Test
    void gzipEncodedResponsesAreDecompressedExactlyOnce() throws Exception {
        try (Buffer body = new Buffer()) {
            body.write(gzip(INFO_JSON));
            server.enqueue(new MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Content-Encoding", "gzip")
                    .body(body)
                    .build());
        }

        final InfoResponse info = buildClient().sync(client -> client.info(), "Failed to read gzip-encoded info response");

        assertThat(info.clusterName())
                .as("A gzip response must be inflated by exactly one layer. If this fails after upgrading "
                        + "opensearch-java to >= 3.6.0, remove disableContentCompression() from "
                        + "OfficialOpensearchClientProvider. If it fails with opensearch-java < 3.6.0, "
                        + "that call went missing and the response is inflated twice.")
                .isEqualTo("test-cluster");
    }

    private OfficialOpensearchClient buildClient() throws Exception {
        final URI uri = server.url("/").uri();
        return new OfficialOpensearchClientProvider(
                ImmutableList.of(uri),
                IndexerJwtAuthToken.disabled(),
                new BasicCredentialsProvider(),
                config(),
                new ObjectMapperProvider().get(),
                null
        ).get();
    }

    private static byte[] gzip(String content) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(out)) {
            gzipOut.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    private static ElasticsearchClientConfiguration config() throws Exception {
        final ElasticsearchClientConfiguration config = new ElasticsearchClientConfiguration();
        new JadConfig(new InMemoryRepository(Map.of(
                "elasticsearch_connect_timeout", "10s",
                "elasticsearch_socket_timeout", "10s"
        )), config).process();
        return config;
    }
}
