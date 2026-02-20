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
package org.graylog.collectors;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoSequenceService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class FleetTransactionLogServiceTest {

    private static final NodeId NODE_ID = new SimpleNodeId("test-node-1");

    private FleetTransactionLogService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        var sequenceService = new MongoSequenceService(
                mongoCollections,
                NODE_ID,
                Set.of(FleetTransactionLogService.SEQUENCE_TOPIC)
        );
        service = new FleetTransactionLogService(mongoCollections, sequenceService, NODE_ID);
    }

    // --- Write path tests ---

    @Test
    void appendFleetMarkerStoresCorrectDocument() {
        long seq = service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);

        assertThat(seq).isEqualTo(1L);

        var doc = service.getCollection().find(Filters.eq("_id", 1L)).first();
        assertThat(doc).isNotNull();
        assertThat(doc.getString(FleetTransactionLogService.FIELD_TARGET)).isEqualTo("fleet");
        assertThat(doc.getList(FleetTransactionLogService.FIELD_TARGET_ID, String.class)).containsExactly("fleet-1");
        assertThat(doc.getString(FleetTransactionLogService.FIELD_TYPE)).isEqualTo("CONFIG_CHANGED");
        assertThat(doc.get(FleetTransactionLogService.FIELD_PAYLOAD)).isNull();
        assertThat(doc.get(FleetTransactionLogService.FIELD_CREATED_AT)).isNotNull();
        assertThat(doc.getString(FleetTransactionLogService.FIELD_CREATED_BY)).isEqualTo("test-node-1");
    }

    @Test
    void appendCollectorMarkerWithPayloadStoresPayload() {
        var payload = new Document("new_fleet_id", "fleet-B");
        long seq = service.appendCollectorMarker(Set.of("inst-1"), MarkerType.FLEET_REASSIGNED, payload);

        assertThat(seq).isEqualTo(1L);

        var doc = service.getCollection().find(Filters.eq("_id", 1L)).first();
        assertThat(doc).isNotNull();
        assertThat(doc.getString(FleetTransactionLogService.FIELD_TARGET)).isEqualTo("collector");
        assertThat(doc.getList(FleetTransactionLogService.FIELD_TARGET_ID, String.class)).containsExactly("inst-1");
        assertThat(doc.getString(FleetTransactionLogService.FIELD_TYPE)).isEqualTo("FLEET_REASSIGNED");
        assertThat(doc.get(FleetTransactionLogService.FIELD_PAYLOAD, Document.class).getString("new_fleet_id"))
                .isEqualTo("fleet-B");
    }

    @Test
    void sequenceNumbersAreMonotonicallyIncreasing() {
        long seq1 = service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);
        long seq2 = service.appendCollectorMarker(Set.of("inst-1"), MarkerType.RESTART, null);
        long seq3 = service.appendFleetMarker("fleet-2", MarkerType.CONFIG_CHANGED);

        assertThat(seq1).isEqualTo(1L);
        assertThat(seq2).isEqualTo(2L);
        assertThat(seq3).isEqualTo(3L);
    }

    // --- Read path tests ---

    @Test
    void getUnprocessedMarkersReturnsByFleetId() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);
        service.appendFleetMarker("fleet-2", MarkerType.CONFIG_CHANGED);

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", null, 0L);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().targetIds()).containsExactly("fleet-1");
    }

    @Test
    void getUnprocessedMarkersReturnsByInstanceUid() {
        service.appendCollectorMarker(Set.of("inst-1"), MarkerType.RESTART, null);
        service.appendCollectorMarker(Set.of("inst-2"), MarkerType.RESTART, null);

        List<TransactionMarker> markers = service.getUnprocessedMarkers(null, "inst-1", 0L);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().targetIds()).containsExactly("inst-1");
    }

    @Test
    void getUnprocessedMarkersReturnsBothScopesWithOr() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED);
        service.appendCollectorMarker(Set.of("inst-1"), MarkerType.RESTART, null);
        service.appendFleetMarker("fleet-2", MarkerType.CONFIG_CHANGED);     // different fleet
        service.appendCollectorMarker(Set.of("inst-2"), MarkerType.RESTART, null);   // different collector

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", "inst-1", 0L);

        assertThat(markers).hasSize(2);
    }

    @Test
    void getUnprocessedMarkersFiltersByLastProcessedSeq() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 1
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 2
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 3

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", null, 2L);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().seq()).isEqualTo(3L);
    }

    @Test
    void getUnprocessedMarkersThrowsWhenBothNull() {
        assertThatThrownBy(() -> service.getUnprocessedMarkers(null, null, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getUnprocessedMarkersReturnsEmptyWhenNoneMatch() {
        service.appendFleetMarker("fleet-1", MarkerType.CONFIG_CHANGED); // seq 1

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", null, 1L);

        assertThat(markers).isEmpty();
    }

    @Test
    void getUnprocessedMarkersDeserializesUnknownTypesAsUnknown() {
        // Insert a marker with an unknown type directly into the collection
        service.getCollection().insertOne(new Document("_id", 999L)
                .append(FleetTransactionLogService.FIELD_TARGET, "fleet")
                .append(FleetTransactionLogService.FIELD_TARGET_ID, List.of("fleet-1"))
                .append(FleetTransactionLogService.FIELD_TYPE, "FUTURE_MARKER_TYPE")
                .append(FleetTransactionLogService.FIELD_CREATED_BY, "test"));

        List<TransactionMarker> markers = service.getUnprocessedMarkers("fleet-1", null, 0L);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().type()).isEqualTo(MarkerType.UNKNOWN);
        assertThat(markers.getFirst().rawType()).isEqualTo("FUTURE_MARKER_TYPE");
    }
}
