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
package org.graylog2.cluster.nodes;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class DataNodeMetadataServiceImplTest {

    private static final String NODE_ID = "test-node-0000-0000-0000-000000000000";

    private DataNodeMetadataService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.service = new DataNodeMetadataServiceImpl(mongoCollections);
    }

    @Test
    void storesVersionWhenNoRecordExists() {
        service.setOpensearchVersion(NODE_ID, "2.19.5");

        assertThat(storedVersion(NODE_ID)).isEqualTo("2.19.5");
    }

    @Test
    void overwritesExistingVersion() {
        service.setOpensearchVersion(NODE_ID, "2.19.5");
        service.setOpensearchVersion(NODE_ID, "2.18.0");

        assertThat(storedVersion(NODE_ID)).isEqualTo("2.18.0");
    }

    @Test
    void storesVersionsForDifferentNodesIndependently() {
        final String otherNodeId = "other-node-0000-0000-0000-000000000000";

        service.setOpensearchVersion(NODE_ID, "2.19.5");
        service.setOpensearchVersion(otherNodeId, "2.18.0");

        assertThat(storedVersion(NODE_ID)).isEqualTo("2.19.5");
        assertThat(storedVersion(otherNodeId)).isEqualTo("2.18.0");
    }

    @Test
    void findByNodeIdReturnsEmptyWhenNoRecordExists() {
        assertThat(service.findByNodeId(NODE_ID)).isEmpty();
    }

    @Test
    void findByNodeIdReturnsMetadataWithCorrectFields() {
        service.setOpensearchVersion(NODE_ID, "2.19.5");

        assertThat(service.findByNodeId(NODE_ID)).hasValueSatisfying(metadata -> {
            assertThat(metadata.nodeId()).isEqualTo(NODE_ID);
            assertThat(metadata.currentOpensearchVersion()).isEqualTo("2.19.5");
        });
    }

    @Test
    void storesLatestAvailableVersion() {
        service.setOpensearchVersion(NODE_ID, "2.18.0");
        service.setLatestAvailableOpensearchVersion(NODE_ID, "2.19.5");

        assertThat(service.findByNodeId(NODE_ID)).hasValueSatisfying(metadata -> {
            assertThat(metadata.currentOpensearchVersion()).isEqualTo("2.18.0");
            assertThat(metadata.latestAvailableOpensearchVersion()).isEqualTo("2.19.5");
        });
    }

    @Test
    void settingOpensearchVersionDoesNotClearLatestAvailableVersion() {
        service.setOpensearchVersion(NODE_ID, "2.18.0");
        service.setLatestAvailableOpensearchVersion(NODE_ID, "2.19.5");
        service.setOpensearchVersion(NODE_ID, "2.19.5");

        assertThat(service.findByNodeId(NODE_ID)).hasValueSatisfying(metadata -> {
            assertThat(metadata.currentOpensearchVersion()).isEqualTo("2.19.5");
            assertThat(metadata.latestAvailableOpensearchVersion()).isEqualTo("2.19.5");
        });
    }

    @Test
    void settingLatestAvailableVersionDoesNotClearOpensearchVersion() {
        service.setOpensearchVersion(NODE_ID, "2.18.0");
        service.setLatestAvailableOpensearchVersion(NODE_ID, "2.19.5");

        assertThat(storedVersion(NODE_ID)).isEqualTo("2.18.0");
    }

    private String storedVersion(String nodeId) {
        return service.findByNodeId(nodeId)
                .map(DataNodeMetadata::currentOpensearchVersion)
                .orElse(null);
    }
}
