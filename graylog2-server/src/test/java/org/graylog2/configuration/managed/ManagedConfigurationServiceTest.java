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
package org.graylog2.configuration.managed;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
class ManagedConfigurationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final String NODE_ID = "node-1";

    private final ClusterEventBus clusterEventBus = mock(ClusterEventBus.class);
    private ManagedConfigurationService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        service = new ServerConfigurationService(mongoCollections, clusterEventBus, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void hasNoEntriesInitially() {
        assertThat(service.get(NODE_ID)).isEmpty();
        assertThat(service.get(null)).isEmpty();
    }

    @Test
    void storesValuesAndPostsOneEvent() {
        final var entries = service.update(NODE_ID, Map.of("b", "2", "a", "1"), "admin");

        assertThat(entries).containsOnlyKeys("a", "b");
        assertThat(entries.get("a").value()).isEqualTo("1");
        assertThat(entries.get("a").updatedBy()).isEqualTo("admin");
        assertThat(entries.get("a").updatedAt()).isEqualTo(NOW);
        assertThat(entries.get("a").effectiveValue()).isEqualTo("1");
        assertThat(entries.get("a").effectiveSource()).isEqualTo(ConfigurationSource.DATABASE);
        verify(clusterEventBus).post(new ManagedConfigurationChangedEvent(NodeType.SERVER, NODE_ID, Set.of("a", "b")));
    }

    @Test
    void mergesChangesAndRemovesNullValues() {
        service.update(NODE_ID, Map.of("a", "1", "b", "2", "c", "3"), "admin");

        final var changes = new HashMap<String, String>();
        changes.put("a", "10");
        changes.put("b", null);
        final var entries = service.update(NODE_ID, changes, "jane");

        assertThat(entries.get("a").value()).isEqualTo("10");
        assertThat(entries.get("a").updatedBy()).isEqualTo("jane");
        assertThat(entries.get("c").value()).isEqualTo("3");
        assertThat(entries.get("c").updatedBy()).isEqualTo("admin");
        // The row stays, it just has no stored value any more
        assertThat(entries).containsKey("b");
        assertThat(entries.get("b").value()).isNull();
        assertThat(entries.get("b").updatedBy()).isNull();
    }

    @Test
    void doesNothingWithoutChanges() {
        assertThat(service.update(NODE_ID, Map.of(), "admin")).isEmpty();

        verify(clusterEventBus, never()).post(any());
    }

    @Test
    void keepsNodeAndClusterScopeApart() {
        service.update(NODE_ID, Map.of("a", "for the node"), "admin");
        service.update(null, Map.of("a", "for the cluster"), "admin");

        assertThat(service.get(NODE_ID).get("a").value()).isEqualTo("for the node");
        assertThat(service.get(null).get("a").value()).isEqualTo("for the cluster");
        assertThat(service.get("another-node")).isEmpty();
    }

    @Test
    void reportedValuesAndStoredValuesDoNotOverwriteEachOther() {
        service.reportNodeValues(NODE_ID, Map.of("a", new ReportedValue("1g", ConfigurationSource.CONFIG_FILE)));
        service.update(NODE_ID, Map.of("a", "2g"), "admin");

        final var entry = service.get(NODE_ID).get("a");
        assertThat(entry.nodeValue()).isEqualTo("1g");
        assertThat(entry.nodeSource()).isEqualTo(ConfigurationSource.CONFIG_FILE);
        assertThat(entry.value()).isEqualTo("2g");
        assertThat(entry.effectiveValue()).isEqualTo("2g");
        assertThat(entry.effectiveSource()).isEqualTo(ConfigurationSource.DATABASE);

        // Reporting again after a restart must not touch what the administrator stored
        service.reportNodeValues(NODE_ID, Map.of("a", new ReportedValue("1g", ConfigurationSource.CONFIG_FILE)));
        assertThat(service.get(NODE_ID).get("a").value()).isEqualTo("2g");
    }

    @Test
    void fallsBackToTheReportedValueWhenTheStoredOneIsRemoved() {
        service.reportNodeValues(NODE_ID, Map.of("a", new ReportedValue("1g", ConfigurationSource.CONFIG_FILE)));
        service.update(NODE_ID, Map.of("a", "2g"), "admin");

        final var changes = new HashMap<String, String>();
        changes.put("a", null);
        final var entry = service.update(NODE_ID, changes, "admin").get("a");

        assertThat(entry.effectiveValue()).isEqualTo("1g");
        assertThat(entry.effectiveSource()).isEqualTo(ConfigurationSource.CONFIG_FILE);
    }

    @Test
    void adoptsAClusterValueOnlyOnce() {
        assertThat(service.adoptClusterValue("a", "first", "node-1")).isTrue();
        assertThat(service.adoptClusterValue("a", "second", "node-2")).isFalse();

        final var entry = service.get(null).get("a");
        assertThat(entry.value()).isEqualTo("first");
        assertThat(entry.updatedBy()).isEqualTo("node-1");
    }

    @Test
    void doesNotAdoptOverAStoredValue() {
        service.update(null, Map.of("a", "set by an admin"), "admin");

        assertThat(service.adoptClusterValue("a", "from the config file", "node-1")).isFalse();
        assertThat(service.get(null).get("a").value()).isEqualTo("set by an admin");
    }

    @Test
    void doesNotAdoptAgainAfterAStoredValueHasBeenRemoved() {
        service.adoptClusterValue("a", "first", "node-1");
        final var changes = new HashMap<String, String>();
        changes.put("a", null);
        service.update(null, changes, "admin");

        assertThat(service.adoptClusterValue("a", "second", "node-2")).isFalse();
        assertThat(service.get(null).get("a").value()).isNull();
    }
}
