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
package org.graylog2.storage.versionprobe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.graylog2.security.JwtSecret;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;

class VersionProbeImplTest {

    public static final String OPENSEARCH_RESPONSE = """
            {
              "name" : "tdvorak-ThinkPad-T14s-Gen-1",
              "cluster_name" : "datanode-cluster",
              "cluster_uuid" : "-PmIXUWGQHukeW7275a9fg",
              "version" : {
                "distribution" : "opensearch",
                "number" : "2.15.0",
                "build_type" : "tar",
                "build_hash" : "61dbcd0795c9bfe9b81e5762175414bc38bbcadf",
                "build_date" : "2024-06-20T03:26:49.193630411Z",
                "build_snapshot" : false,
                "lucene_version" : "9.10.0",
                "minimum_wire_compatibility_version" : "7.10.0",
                "minimum_index_compatibility_version" : "7.0.0"
              },
              "tagline" : "The OpenSearch Project: https://opensearch.org/"
            }
            """;
    private final MockWebServer server = new MockWebServer();

    @BeforeEach
    void setUp() throws IOException {
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void testSuccessfulVersionProbe() throws URISyntaxException, IOException {
        server.enqueue(new MockResponse().setBody(OPENSEARCH_RESPONSE));
        final CollectingVersionProbeListener versionProbeListener = new CollectingVersionProbeListener();
        final VersionProbe versionProbe = new VersionProbeImpl(objectMapper(), okHttpClient(), jwtTokenProvider(randomSecret()), 100, Duration.milliseconds(10), false, versionProbeListener);
        final Optional<SearchVersion> probedVersion = versionProbe.probe(Collections.singleton(server.url("/").url().toURI()));
        Assertions.assertThat(probedVersion)
                .isPresent()
                .hasValueSatisfying(searchVersion -> Assertions.assertThat(searchVersion).isEqualTo(SearchVersion.opensearch("2.15.0")));

        Assertions.assertThat(versionProbeListener.getErrors()).isEmpty();
        Assertions.assertThat(versionProbeListener.getRetries()).isEmpty();

    }

    @Test
    void testFailingVersionProbe() {

        server.setDispatcher(alwaysUnauthorized());

        final CollectingVersionProbeListener versionProbeListener = new CollectingVersionProbeListener();
        final VersionProbe versionProbe = new VersionProbeImpl(objectMapper(), okHttpClient(), jwtTokenProvider(randomSecret()), 3, Duration.milliseconds(10), false, versionProbeListener);
        final Optional<SearchVersion> probedVersion = versionProbe.probe(Collections.singleton(server.url("/").uri()));
        Assertions.assertThat(probedVersion)
                .isEmpty();

        Assertions.assertThat(versionProbeListener.getErrors())
                .hasSize(4)
                .contains("Unable to retrieve version from indexer node: Retrying failed to complete successfully after 3 attempts.")
                .anySatisfy(error -> Assertions.assertThat(error).contains("an exception occurred while deserializing error response"));

    }

    @Nonnull
    private static Dispatcher alwaysUnauthorized() {
        return new Dispatcher() {
            @Nonnull
            @Override
            public MockResponse dispatch(@Nonnull RecordedRequest recordedRequest) throws InterruptedException {
                return new MockResponse()
                        .setBody("unauthorized")
                        .setResponseCode(Response.Status.UNAUTHORIZED.getStatusCode());
            }
        };
    }

    @Test
    void testVersionProbeWithJwtAuth() {

        final JwtSecret secret = randomSecret();

        server.setDispatcher(jwtCheckingDispatcher(secret));

        final CollectingVersionProbeListener versionProbeListener = new CollectingVersionProbeListener();

        final VersionProbe versionProbe = new VersionProbeImpl(objectMapper(), okHttpClient(), jwtTokenProvider(secret), 3, Duration.milliseconds(10), true, versionProbeListener);
        final Optional<SearchVersion> probedVersion = versionProbe.probe(Collections.singleton(server.url("/").uri()));
        Assertions.assertThat(probedVersion)
                .isPresent()
                .hasValueSatisfying(searchVersion -> Assertions.assertThat(searchVersion).isEqualTo(SearchVersion.opensearch("2.15.0")));

        Assertions.assertThat(versionProbeListener.getErrors()).isEmpty();
        Assertions.assertThat(versionProbeListener.getRetries()).isEmpty();

        final CollectingVersionProbeListener versionProbeListenerWithWrongSecret = new CollectingVersionProbeListener();
        final VersionProbe versionProbeWithWrongSecret = new VersionProbeImpl(objectMapper(), okHttpClient(), jwtTokenProvider(randomSecret()), 3, Duration.milliseconds(10), true, versionProbeListenerWithWrongSecret);
        final Optional<SearchVersion> probedVersionWithWrongSecret = versionProbeWithWrongSecret.probe(Collections.singleton(server.url("/").uri()));
        Assertions.assertThat(probedVersionWithWrongSecret)
                .isEmpty();

        Assertions.assertThat(versionProbeListenerWithWrongSecret.getErrors()).hasSize(4)
                .contains("Unable to retrieve version from indexer node: Retrying failed to complete successfully after 3 attempts.")
                .anySatisfy(error -> Assertions.assertThat(error).contains("an exception occurred while deserializing error response"));
        Assertions.assertThat(versionProbeListenerWithWrongSecret.getRetries()).hasSize(3);

    }

    @Nonnull
    private static Dispatcher jwtCheckingDispatcher(JwtSecret secret) {
        return new Dispatcher() {
            @Nonnull
            @Override
            public MockResponse dispatch(@Nonnull RecordedRequest recordedRequest) {
                final Optional<? extends Jwt<?, ?>> parsedToken = Optional.ofNullable(recordedRequest.getHeaders().get(HttpHeaders.AUTHORIZATION))
                        .map(header -> header.substring("Bearer ".length()))
                        .flatMap(token -> {
                            final JwtParser parser = Jwts.parser()
                                    .verifyWith(secret.getSigningKey())
                                    .requireSubject("admin")
                                    .requireIssuer("graylog")
                                    .build();
                            try {
                                return Optional.of(parser.parse(token));
                            } catch (Throwable e) {
                                return Optional.empty();
                            }
                        });

                if (parsedToken.isEmpty()) {
                    return new MockResponse()
                            .setBody("Failed to parse auth header")
                            .setResponseCode(Response.Status.UNAUTHORIZED.getStatusCode());
                } else {
                    return new MockResponse().setBody(OPENSEARCH_RESPONSE);
                }
            }
        };
    }

    @Nonnull
    private static JwtSecret randomSecret() {
        return new JwtSecret(RandomStringUtils.randomAlphabetic(96));
    }


    @Nonnull
    private static IndexerJwtAuthTokenProvider jwtTokenProvider(JwtSecret secret) {
        return new IndexerJwtAuthTokenProvider(
                secret,
                Duration.seconds(60),
                Duration.seconds(30)
        );
    }

    @Nonnull
    private static OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder().build();
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapperProvider().get();
    }

}
