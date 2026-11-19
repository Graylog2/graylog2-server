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
package org.graylog.datanode.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.datanode.rest.RepositoryClientsResponse.Client;
import org.graylog.datanode.rest.RepositoryClientsResponse.Repository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryClientsResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // GET _snapshot
    private static final String REPOSITORIES = """
            {
              "archive-eu": {"type": "s3", "settings": {"bucket": "b", "client": "repository_archive-eu_7f3a"}},
              "azure-archive": {"type": "azure", "settings": {"container": "c", "client": "repository_azure-archive_0002"}},
              "legacy": {"type": "s3", "settings": {"bucket": "b"}},
              "local": {"type": "fs", "settings": {"location": "/snapshots"}}
            }
            """;

    // GET _nodes/settings?flat_settings=true&filter_path=nodes.*.name,nodes.*.settings
    private static final String NODES = """
            {
              "nodes": {
                "a1": {
                  "name": "datanode-1",
                  "settings": {
                    "path.repo": "/snapshots",
                    "s3.client.repository_archive-eu_7f3a.endpoint": "minio.local:9000",
                    "s3.client.repository_archive-eu_7f3a.protocol": "http",
                    "s3.client.default.endpoint": "s3.amazonaws.com",
                    "azure.client.repository_azure-archive_0002.endpoint_suffix": "core.windows.net"
                  }
                },
                "b2": {
                  "name": "datanode-2",
                  "settings": {
                    "s3.client.repository_archive-eu_7f3a.endpoint": "minio.old:9000",
                    "s3.client.repository_archive-eu_7f3a.protocol": "http",
                    "s3.client.default.endpoint": "s3.amazonaws.com"
                  }
                }
              }
            }
            """;

    @Test
    void repositoriesWithTheirClients() throws Exception {
        assertThat(response().repositories()).containsExactly(
                new Repository("archive-eu", "s3", "repository_archive-eu_7f3a", List.of()),
                new Repository("azure-archive", "azure", "repository_azure-archive_0002", List.of("datanode-2")),
                // The default client may be configured in the keystore only, opensearch doesn't tell
                new Repository("legacy", "s3", "default", null),
                new Repository("local", "fs", null, null));
    }

    @Test
    void clientsByConfiguration() throws Exception {
        assertThat(response().clients()).containsExactly(
                new Client("azure", "repository_azure-archive_0002", Map.of("endpoint_suffix", "core.windows.net"), List.of("datanode-1")),
                new Client("s3", "default", Map.of("endpoint", "s3.amazonaws.com"), List.of("datanode-1", "datanode-2")),
                // A node that hasn't restarted since the client changed
                new Client("s3", "repository_archive-eu_7f3a", Map.of("endpoint", "minio.local:9000", "protocol", "http"), List.of("datanode-1")),
                new Client("s3", "repository_archive-eu_7f3a", Map.of("endpoint", "minio.old:9000", "protocol", "http"), List.of("datanode-2")));
    }

    @Test
    void emptyCluster() throws Exception {
        final RepositoryClientsResponse response = RepositoryClientsResponse.from(json("{}"), json("{}"));

        assertThat(response.repositories()).isEmpty();
        assertThat(response.clients()).isEmpty();
    }

    private static RepositoryClientsResponse response() throws Exception {
        return RepositoryClientsResponse.from(json(REPOSITORIES), json(NODES));
    }

    private static JsonNode json(String json) throws Exception {
        return OBJECT_MAPPER.readTree(json);
    }
}
