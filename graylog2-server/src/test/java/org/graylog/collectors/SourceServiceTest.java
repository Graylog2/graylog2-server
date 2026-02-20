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

import org.graylog.collectors.db.FileSourceConfig;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.SourceConfig;
import org.graylog.collectors.db.SourceDTO;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoSequenceService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class SourceServiceTest {

    private static final NodeId NODE_ID = new SimpleNodeId("test-node-1");

    private FleetService fleetService;
    private SourceService sourceService;
    private FleetTransactionLogService txnLogService;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        var sequenceService = new MongoSequenceService(
                mongoCollections,
                NODE_ID,
                Set.of(FleetTransactionLogService.SEQUENCE_TOPIC)
        );
        txnLogService = new FleetTransactionLogService(mongoCollections, sequenceService, NODE_ID);
        sourceService = new SourceService(mongoCollections, txnLogService);
        fleetService = new FleetService(mongoCollections, txnLogService, sourceService);
    }

    private SourceConfig validFileConfig() {
        return FileSourceConfig.builder().paths(List.of("/var/log/syslog")).readMode("tail").build();
    }

    @Test
    void createSource() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);

        SourceDTO source = sourceService.create(fleet.id(), "my-source", "A source", true, validFileConfig());

        assertThat(source.id()).isNotNull();
        assertThat(source.fleetId()).isEqualTo(fleet.id());
        assertThat(source.name()).isEqualTo("my-source");
        assertThat(source.description()).isEqualTo("A source");
        assertThat(source.enabled()).isTrue();
        assertThat(source.config().type()).isEqualTo("file");
    }

    @Test
    void createSourceAppendsConfigChangedMarker() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);

        // Fleet creation already appends a marker; get markers after fleet creation
        List<TransactionMarker> markersAfterFleet = txnLogService.getUnprocessedMarkers(fleet.id(), null, 0L);
        long lastSeq = markersAfterFleet.getLast().seq();

        sourceService.create(fleet.id(), "my-source", "A source", true, validFileConfig());

        List<TransactionMarker> markers = txnLogService.getUnprocessedMarkers(fleet.id(), null, lastSeq);
        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().type()).isEqualTo(MarkerType.CONFIG_CHANGED);
    }

    @Test
    void getSource() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);
        SourceDTO created = sourceService.create(fleet.id(), "my-source", "A source", true, validFileConfig());

        Optional<SourceDTO> result = sourceService.get(fleet.id(), created.id());

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("my-source");
    }

    @Test
    void getSourceWrongFleet() {
        FleetDTO fleetA = fleetService.create("fleet-a", "Fleet A", null);
        FleetDTO fleetB = fleetService.create("fleet-b", "Fleet B", null);
        SourceDTO source = sourceService.create(fleetA.id(), "my-source", "A source", true, validFileConfig());

        Optional<SourceDTO> result = sourceService.get(fleetB.id(), source.id());

        assertThat(result).isEmpty();
    }

    @Test
    void updateSource() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);
        SourceDTO created = sourceService.create(fleet.id(), "my-source", "Original desc", true, validFileConfig());

        SourceConfig newConfig = FileSourceConfig.builder().paths(List.of("/var/log/auth.log")).readMode("tail").build();
        Optional<SourceDTO> updated = sourceService.update(fleet.id(), created.id(), "updated-source", "Updated desc", false, newConfig);

        assertThat(updated).isPresent();
        assertThat(updated.get().name()).isEqualTo("updated-source");
        assertThat(updated.get().description()).isEqualTo("Updated desc");
        assertThat(updated.get().enabled()).isFalse();
        assertThat(((FileSourceConfig) updated.get().config()).paths()).containsExactly("/var/log/auth.log");
        assertThat(updated.get().config().type()).isEqualTo("file");
    }

    @Test
    void deleteSource() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);
        SourceDTO created = sourceService.create(fleet.id(), "my-source", "A source", true, validFileConfig());

        boolean deleted = sourceService.delete(fleet.id(), created.id());

        assertThat(deleted).isTrue();
        assertThat(sourceService.get(fleet.id(), created.id())).isEmpty();
    }

    @Test
    void deleteSourceWrongFleet() {
        FleetDTO fleetA = fleetService.create("fleet-a", "Fleet A", null);
        FleetDTO fleetB = fleetService.create("fleet-b", "Fleet B", null);
        SourceDTO source = sourceService.create(fleetA.id(), "my-source", "A source", true, validFileConfig());

        boolean deleted = sourceService.delete(fleetB.id(), source.id());

        assertThat(deleted).isFalse();
        // Source should still exist in fleet A
        assertThat(sourceService.get(fleetA.id(), source.id())).isPresent();
    }

    @Test
    void findByFleet() {
        FleetDTO fleetA = fleetService.create("fleet-a", "Fleet A", null);
        FleetDTO fleetB = fleetService.create("fleet-b", "Fleet B", null);
        sourceService.create(fleetA.id(), "source-a1", "Source A1", true, validFileConfig());
        sourceService.create(fleetA.id(), "source-a2", "Source A2", true, validFileConfig());
        sourceService.create(fleetB.id(), "source-b1", "Source B1", true, validFileConfig());

        SearchQuery query = sourceService.parseSearchQuery("");
        PaginatedList<SourceDTO> result = sourceService.findByFleet(fleetA.id(), query, 1, 10,
                SourceDTO.FIELD_NAME, SortOrder.ASCENDING, source -> true);

        assertThat(result.pagination().total()).isEqualTo(2);
        assertThat(result.delegate()).hasSize(2);
        assertThat(result.delegate()).extracting(SourceDTO::name)
                .containsExactly("source-a1", "source-a2");
    }

    @Test
    void streamAllByFleet() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);
        sourceService.create(fleet.id(), "source-1", "Source 1", true, validFileConfig());
        sourceService.create(fleet.id(), "source-2", "Source 2", true, validFileConfig());

        try (final var stream = sourceService.streamAllByFleet(fleet.id())) {
            assertThat(stream).hasSize(2);
        }
    }

    @Test
    void deleteAllByFleet() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);
        sourceService.create(fleet.id(), "source-1", "Source 1", true, validFileConfig());
        sourceService.create(fleet.id(), "source-2", "Source 2", true, validFileConfig());

        long deleted = sourceService.deleteAllByFleet(fleet.id());

        assertThat(deleted).isEqualTo(2);
        try (final var stream = sourceService.streamAllByFleet(fleet.id())) {
            assertThat(stream).isEmpty();
        }
    }

    @Test
    void duplicateNameWithinFleetThrows() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);
        sourceService.create(fleet.id(), "my-source", "First source", true, validFileConfig());

        assertThatThrownBy(() -> sourceService.create(fleet.id(), "my-source", "Second source", true, validFileConfig()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sameNameDifferentFleetsAllowed() {
        FleetDTO fleetA = fleetService.create("fleet-a", "Fleet A", null);
        FleetDTO fleetB = fleetService.create("fleet-b", "Fleet B", null);

        SourceDTO sourceA = sourceService.create(fleetA.id(), "my-source", "Source in A", true, validFileConfig());
        SourceDTO sourceB = sourceService.create(fleetB.id(), "my-source", "Source in B", true, validFileConfig());

        assertThat(sourceA.id()).isNotEqualTo(sourceB.id());
        assertThat(sourceA.name()).isEqualTo(sourceB.name());
    }

    @Test
    void createSourceWithInvalidConfigThrows() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);
        SourceConfig invalidConfig = FileSourceConfig.builder().paths(List.of()).readMode("tail").build();

        assertThatThrownBy(() -> sourceService.create(fleet.id(), "my-source", "A source", true, invalidConfig))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
