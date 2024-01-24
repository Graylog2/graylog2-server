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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.OpenSearchVersionInfo;
import org.opensearch.client.opensearch.core.InfoResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NodeAdapterOS2Test {

    private NodeAdapterOS2 toTest;
    private OpenSearchClient openSearchClient;
    private Request request;
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @BeforeEach
    void setUp() {
        openSearchClient = mock(OpenSearchClient.class);
        toTest = new NodeAdapterOS2(openSearchClient);
        request = new Request("GET", "/?filter_path=version.number,version.distribution");
    }

    @Test
    void testElasticsearchVersionFetching() {
        mockResponse(responseBuilder()
                .version(versionBuilder()
                        .distribution("Elasticsearch")
                        .number("7.10.2")
                        .build()).build());

        assertThat(toTest.version())
                .isNotEmpty()
                .contains(SearchVersion.create(SearchVersion.Distribution.ELASTICSEARCH, Version.valueOf("7.10.2")));

    }

    @Test
    void testOpensearchVersionFetching() {
        mockResponse(responseBuilder()
                .version(versionBuilder()
                        .distribution("opensearch")
                        .number("1.3.1")
                        .build())
                .build());

        assertThat(toTest.version())
                .isNotEmpty()
                .contains(SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.valueOf("1.3.1")));

    }

    private InfoResponse.Builder responseBuilder() {
        return new InfoResponse.Builder()
                .name("node01")
                .clusterName("testcluster")
                .clusterUuid("deadbeef")
                .tagline("The best product for search");
    }

    private OpenSearchVersionInfo.Builder versionBuilder() {
        return new OpenSearchVersionInfo.Builder()
                .buildDate("2021-12-01")
                .buildHash("deadbeef")
                .buildSnapshot(false)
                .buildType("release")
                .luceneVersion("9.0.0")
                .minimumWireCompatibilityVersion("7.0.0")
                .minimumIndexCompatibilityVersion("7.0.0");
    }

    private void mockResponse(final InfoResponse response) {
        when(openSearchClient.execute(any(ThrowingFunction.class))).thenReturn(response);
    }
}
