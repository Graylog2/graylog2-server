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
package org.graylog2.rest.resources.datanodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataNodeWithOpensearchVersionTest {

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private static DataNodeDto dataNode() {
        return DataNodeDto.builder()
                .setId("node-id")
                .setHostname("datanode1.example.com")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .setDatanodeVersion("7.2.0")
                .build();
    }

    @Test
    void serializesOpensearchVersionAlongsideUnwrappedDataNodeFields() {
        final JsonNode json = objectMapper.valueToTree(new DataNodeWithOpensearchVersion(dataNode(), "2.19.5"));

        assertThat(json.path("opensearch_version").asText()).isEqualTo("2.19.5");
        // the data node itself must stay flat, no nesting introduced by the wrapper
        assertThat(json.path("hostname").asText()).isEqualTo("datanode1.example.com");
        assertThat(json.path("node_id").asText()).isEqualTo("node-id");
        assertThat(json.path("datanode_version").asText()).isEqualTo("7.2.0");
        assertThat(json.path("datanode_status").asText()).isEqualTo("AVAILABLE");
        assertThat(json.has("data_node")).isFalse();
    }

    @Test
    void serializesNullOpensearchVersionForNodesWithoutMetadata() {
        final JsonNode json = objectMapper.valueToTree(new DataNodeWithOpensearchVersion(dataNode(), null));

        assertThat(json.path("opensearch_version").isNull()).isTrue();
        assertThat(json.path("hostname").asText()).isEqualTo("datanode1.example.com");
    }
}
