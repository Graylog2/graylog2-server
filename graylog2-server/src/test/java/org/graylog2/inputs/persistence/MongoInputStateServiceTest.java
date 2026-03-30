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
package org.graylog2.inputs.persistence;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.system.SimpleNodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class MongoInputStateServiceTest {

    private static final String NODE_1 = "node-1";
    private static final String NODE_2 = "node-2";

    private MongoInputStateService serviceNode1;
    private MongoInputStateService serviceNode2;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        serviceNode1 = new MongoInputStateService(mongoCollections, new SimpleNodeId(NODE_1));
        serviceNode2 = new MongoInputStateService(mongoCollections, new SimpleNodeId(NODE_2));
    }

    @Test
    void upsertAndGetByState() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        serviceNode1.upsertState("input-2", IOState.Type.FAILED, now, now, "connection refused");

        assertThat(serviceNode1.getByState(IOState.Type.RUNNING)).containsExactly("input-1");
        assertThat(serviceNode1.getByState(IOState.Type.FAILED)).containsExactly("input-2");
        assertThat(serviceNode1.getByState(IOState.Type.STOPPED)).isEmpty();
    }

    @Test
    void upsertOverwritesExistingState() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.STARTING, now, null, null);
        assertThat(serviceNode1.getByState(IOState.Type.STARTING)).containsExactly("input-1");

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        assertThat(serviceNode1.getByState(IOState.Type.RUNNING)).containsExactly("input-1");
        assertThat(serviceNode1.getByState(IOState.Type.STARTING)).isEmpty();
    }

    @Test
    void clusterStatusesAggregatesAcrossNodes() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        serviceNode2.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        serviceNode1.upsertState("input-2", IOState.Type.FAILED, now, now, "error");

        Map<String, Set<String>> statuses = serviceNode1.getClusterStatuses();

        assertThat(statuses).containsKey("input-1");
        assertThat(statuses.get("input-1")).containsExactly("RUNNING");
        assertThat(statuses).containsKey("input-2");
        assertThat(statuses.get("input-2")).containsExactly("FAILED");
    }

    @Test
    void globalInputDifferentStatesOnDifferentNodes() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("global-input", IOState.Type.RUNNING, now, null, null);
        serviceNode2.upsertState("global-input", IOState.Type.FAILED, now, now, "error");

        Map<String, Set<String>> statuses = serviceNode1.getClusterStatuses();
        assertThat(statuses.get("global-input")).containsExactlyInAnyOrder("RUNNING", "FAILED");

        // getByState should find this input under both states
        assertThat(serviceNode1.getByState(IOState.Type.RUNNING)).contains("global-input");
        assertThat(serviceNode1.getByState(IOState.Type.FAILED)).contains("global-input");
    }

    @Test
    void removeState() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        assertThat(serviceNode1.getByState(IOState.Type.RUNNING)).containsExactly("input-1");

        serviceNode1.removeState("input-1");
        assertThat(serviceNode1.getByState(IOState.Type.RUNNING)).isEmpty();
    }

    @Test
    void removeStateOnlyAffectsOwnNode() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        serviceNode2.upsertState("input-1", IOState.Type.RUNNING, now, null, null);

        serviceNode1.removeState("input-1");

        // Node 2 still has the state
        assertThat(serviceNode1.getByState(IOState.Type.RUNNING)).containsExactly("input-1");
    }

    @Test
    void removeAllForNode() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        serviceNode1.upsertState("input-2", IOState.Type.RUNNING, now, null, null);
        serviceNode2.upsertState("input-3", IOState.Type.RUNNING, now, null, null);

        serviceNode1.removeAllForNode();

        // Only node 2's states remain
        assertThat(serviceNode1.getByState(IOState.Type.RUNNING)).containsExactly("input-3");
    }

    @Test
    void removeAllForSpecificNode() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        serviceNode2.upsertState("input-2", IOState.Type.RUNNING, now, null, null);

        // Clean up node-2 from node-1's service (stale cleanup scenario)
        serviceNode1.removeAllForNode(NODE_2);

        assertThat(serviceNode1.getByState(IOState.Type.RUNNING)).containsExactly("input-1");
    }

    @Test
    void getDistinctNodeIds() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        serviceNode2.upsertState("input-2", IOState.Type.RUNNING, now, null, null);

        assertThat(serviceNode1.getDistinctNodeIds()).containsExactlyInAnyOrder(NODE_1, NODE_2);
    }

    @Test
    void emptyCollectionReturnsEmptyResults() {
        assertThat(serviceNode1.getByState(IOState.Type.RUNNING)).isEmpty();
        assertThat(serviceNode1.getClusterStatuses()).isEmpty();
        assertThat(serviceNode1.getDistinctNodeIds()).isEmpty();
    }

    @Test
    void getByStatesReturnsFullDtos() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);
        serviceNode1.upsertState("input-2", IOState.Type.FAILED, now, now, "connection refused");
        serviceNode2.upsertState("input-3", IOState.Type.RUNNING, now, null, null);
        serviceNode1.upsertState("input-4", IOState.Type.STOPPED, now, null, null);

        Set<InputStateDto> results = serviceNode1.getByStates(Set.of(IOState.Type.RUNNING, IOState.Type.FAILED));

        assertThat(results).hasSize(3);
        assertThat(results).extracting(InputStateDto::inputId)
                .containsExactlyInAnyOrder("input-1", "input-2", "input-3");
        assertThat(results).allSatisfy(dto -> assertThat(dto.nodeId()).isIn(NODE_1, NODE_2));
    }

    @Test
    void getByStatesReturnsEmptyForNoMatches() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        serviceNode1.upsertState("input-1", IOState.Type.RUNNING, now, null, null);

        Set<InputStateDto> results = serviceNode1.getByStates(Set.of(IOState.Type.FAILED, IOState.Type.STOPPED));
        assertThat(results).isEmpty();
    }
}
