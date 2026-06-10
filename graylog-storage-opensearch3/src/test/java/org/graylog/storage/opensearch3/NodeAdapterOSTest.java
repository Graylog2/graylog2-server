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

import com.github.zafarkhaja.semver.Version;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch._types.OpenSearchVersionInfo;
import org.opensearch.client.opensearch.core.InfoResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class NodeAdapterOSTest {

    private NodeAdapterOS toTest;
    @Mock
    private OfficialOpensearchClient client;
    @Mock
    private OpenSearchVersionInfo versionInfoMock;

    @BeforeEach
    void setUp() {
        toTest = new NodeAdapterOS(client);

    }

    @Test
    void testElasticsearchVersionFetching() {
        doReturn("7.10.2").when(versionInfoMock).number();
        doReturn("elasticsearch").when(versionInfoMock).distribution();
        doReturn(InfoResponse.builder()
                .version(versionInfoMock)
                .clusterName("General Cluster")
                .clusterUuid("1842")
                .name("Graylog")
                .tagline("?")
                .build())
                .when(client).execute(any(), anyString());

        assertThat(toTest.version())
                .isNotEmpty()
                .contains(SearchVersion.create(SearchVersion.Distribution.ELASTICSEARCH, Version.parse("7.10.2")));

    }

    @Test
    void testOpensearchVersionFetching() throws IOException {
        doReturn("3.3.1").when(versionInfoMock).number();
        doReturn("opensearch").when(versionInfoMock).distribution();
        doReturn(InfoResponse.builder()
                .version(versionInfoMock)
                .clusterName("General Cluster")
                .clusterUuid("1842")
                .name("Graylog")
                .tagline("The OpenSearch Project: https://opensearch.org/")
                .build())
                .when(client).execute(any(), anyString());
        assertThat(toTest.version())
                .isNotEmpty()
                .contains(SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.parse("3.3.1")));

    }

    @Test
    void testUnnamedDistributionVersionFetching() {
        doReturn("7.10.2").when(versionInfoMock).number();
        doReturn(InfoResponse.builder()
                .version(versionInfoMock)
                .clusterName("General Cluster")
                .clusterUuid("1842")
                .name("Graylog")
                .tagline("?")
                .build())
                .when(client).execute(any(), anyString());

        assertThat(toTest.version())
                .isNotEmpty()
                .contains(SearchVersion.create(SearchVersion.Distribution.ELASTICSEARCH, Version.parse("7.10.2")));

    }

}
