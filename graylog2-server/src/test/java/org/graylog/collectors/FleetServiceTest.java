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
import org.graylog.collectors.db.FleetConfig;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.SourceConfig;
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
class FleetServiceTest {

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
        return new FileSourceConfig(List.of("/var/log/syslog"), "tail", null);
    }

    @Test
    void createFleet() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", "1.0.0");

        assertThat(fleet.id()).isNotNull();
        assertThat(fleet.name()).isEqualTo("test-fleet");
        assertThat(fleet.description()).isEqualTo("A test fleet");
        assertThat(fleet.targetVersion()).isEqualTo("1.0.0");
        assertThat(fleet.createdAt()).isNotNull();
        assertThat(fleet.updatedAt()).isNotNull();
    }

    @Test
    void createFleetAppendsConfigChangedMarker() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);

        List<TransactionMarker> markers = txnLogService.getUnprocessedMarkers(fleet.id(), null, 0L);
        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().type()).isEqualTo(MarkerType.CONFIG_CHANGED);
    }

    @Test
    void getFleet() {
        FleetDTO created = fleetService.create("test-fleet", "A test fleet", "2.0.0");

        Optional<FleetDTO> result = fleetService.get(created.id());

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("test-fleet");
        assertThat(result.get().description()).isEqualTo("A test fleet");
        assertThat(result.get().targetVersion()).isEqualTo("2.0.0");
    }

    @Test
    void getFleetNotFound() {
        Optional<FleetDTO> result = fleetService.get("aaaaaaaaaaaaaaaaaaaaaaaa");

        assertThat(result).isEmpty();
    }

    @Test
    void updateFleet() {
        FleetDTO created = fleetService.create("test-fleet", "Original desc", "1.0.0");

        Optional<FleetDTO> updated = fleetService.update(created.id(), "updated-fleet", "Updated desc", "2.0.0");

        assertThat(updated).isPresent();
        assertThat(updated.get().name()).isEqualTo("updated-fleet");
        assertThat(updated.get().description()).isEqualTo("Updated desc");
        assertThat(updated.get().targetVersion()).isEqualTo("2.0.0");
        assertThat(updated.get().updatedAt()).isAfterOrEqualTo(created.updatedAt());
    }

    @Test
    void deleteFleet() {
        FleetDTO created = fleetService.create("test-fleet", "A test fleet", null);

        boolean deleted = fleetService.delete(created.id());

        assertThat(deleted).isTrue();
        assertThat(fleetService.get(created.id())).isEmpty();
    }

    @Test
    void deleteFleetNotFound() {
        boolean deleted = fleetService.delete("aaaaaaaaaaaaaaaaaaaaaaaa");

        assertThat(deleted).isFalse();
    }

    @Test
    void duplicateNameThrowsException() {
        fleetService.create("test-fleet", "First fleet", null);

        assertThatThrownBy(() -> fleetService.create("test-fleet", "Second fleet", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findPaginated() {
        fleetService.create("fleet-a", "First", null);
        fleetService.create("fleet-b", "Second", null);
        fleetService.create("fleet-c", "Third", null);

        SearchQuery query = fleetService.parseSearchQuery("");
        PaginatedList<FleetDTO> result = fleetService.findPaginated(query, 1, 2,
                FleetDTO.FIELD_NAME, SortOrder.ASCENDING);

        assertThat(result.pagination().total()).isEqualTo(3);
        assertThat(result.delegate()).hasSize(2);
    }

    @Test
    void findPaginatedWithSearch() {
        fleetService.create("alpha-fleet", "First fleet", null);
        fleetService.create("beta-fleet", "Second fleet", null);

        SearchQuery query = fleetService.parseSearchQuery("alpha");
        PaginatedList<FleetDTO> result = fleetService.findPaginated(query, 1, 10,
                FleetDTO.FIELD_NAME, SortOrder.ASCENDING);

        assertThat(result.pagination().total()).isEqualTo(1);
        assertThat(result.delegate()).hasSize(1);
        assertThat(result.delegate().getFirst().name()).isEqualTo("alpha-fleet");
    }

    @Test
    void assembleConfigReturnsFleetWithSources() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", "1.0.0");
        sourceService.create(fleet.id(), "source-1", "First source", true, validFileConfig());
        sourceService.create(fleet.id(), "source-2", "Second source", true, validFileConfig());

        Optional<FleetConfig> config = fleetService.assembleConfig(fleet.id());

        assertThat(config).isPresent();
        assertThat(config.get().fleet().name()).isEqualTo("test-fleet");
        assertThat(config.get().sources()).hasSize(2);
    }

    @Test
    void assembleConfigForDeletedFleetReturnsEmpty() {
        Optional<FleetConfig> config = fleetService.assembleConfig("aaaaaaaaaaaaaaaaaaaaaaaa");

        assertThat(config).isEmpty();
    }

    @Test
    void deleteFleetCascadesSourceDeletion() {
        FleetDTO fleet = fleetService.create("test-fleet", "A test fleet", null);
        sourceService.create(fleet.id(), "source-1", "First source", true, validFileConfig());
        sourceService.create(fleet.id(), "source-2", "Second source", true, validFileConfig());

        // Simulate the cascade delete as done by FleetResource
        sourceService.deleteAllByFleet(fleet.id());
        fleetService.delete(fleet.id());

        assertThat(fleetService.get(fleet.id())).isEmpty();
        assertThat(sourceService.listAllByFleet(fleet.id())).isEmpty();
    }
}
