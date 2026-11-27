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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PendingRestartsTest {
    private static final Instant CHANGED_AT = Instant.parse("2026-11-16T10:00:00Z");
    private static final Instant BEFORE_CHANGE = CHANGED_AT.minusSeconds(60);
    private static final Instant AFTER_CHANGE = CHANGED_AT.plusSeconds(60);
    private static final Instant NOW = CHANGED_AT.plusSeconds(120);

    private final ManagedConfigurationService configurationService = mock(ManagedConfigurationService.class);
    private PendingRestarts pendingRestarts;

    @BeforeEach
    void setUp() {
        final var catalog = new ConfigurationCatalog(Set.of(
                ConfigurationParameter.builder(NodeType.DATANODE, "per_node", "Per node").build(),
                ConfigurationParameter.builder(NodeType.DATANODE, "cluster_wide", "Cluster wide")
                        .scope(ParameterScope.CLUSTER).build(),
                ConfigurationParameter.builder(NodeType.DATANODE, "applied_live", "Applied live")
                        .requiresRestart(false).build()));
        final var configurationServices = mock(ManagedConfigurationServices.class);
        when(configurationServices.get(NodeType.DATANODE)).thenReturn(configurationService);
        pendingRestarts = new PendingRestarts(catalog, configurationServices, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ManagedConfigurationEntry entry(String nodeId, String name, Instant changedAt) {
        return new ManagedConfigurationEntry(null, nodeId, name, null, null, null, "a value", changedAt, "admin",
                changedAt);
    }

    private static NodeConfigurationState appliedAt(String nodeId, Instant appliedAt) {
        return new NodeConfigurationState(null, nodeId, appliedAt, appliedAt, null);
    }

    private void storedForCluster(ManagedConfigurationEntry... entries) {
        when(configurationService.get(null)).thenReturn(byName(entries));
    }

    private void storedForNode(String nodeId, ManagedConfigurationEntry... entries) {
        when(configurationService.get(nodeId)).thenReturn(byName(entries));
    }

    private static Map<String, ManagedConfigurationEntry> byName(ManagedConfigurationEntry... entries) {
        return List.of(entries).stream()
                .collect(Collectors.toMap(ManagedConfigurationEntry::name, entry -> entry));
    }

    @Test
    void valueChangedAfterTheNodeAppliedItsConfigurationIsPending() {
        storedForNode("node-1", entry("node-1", "per_node", CHANGED_AT));
        when(configurationService.configurationStates())
                .thenReturn(Map.of("node-1", appliedAt("node-1", BEFORE_CHANGE)));

        assertThat(pendingRestarts.pendingParameters(NodeType.DATANODE, List.of("node-1")))
                .containsExactly(Map.entry("node-1", Set.of("per_node")));
    }

    @Test
    void valueChangedBeforeTheNodeAppliedItsConfigurationIsNotPending() {
        storedForNode("node-1", entry("node-1", "per_node", CHANGED_AT));
        when(configurationService.configurationStates())
                .thenReturn(Map.of("node-1", appliedAt("node-1", AFTER_CHANGE)));

        assertThat(pendingRestarts.pendingParameters(NodeType.DATANODE, List.of("node-1")).get("node-1")).isEmpty();
    }

    @Test
    void clusterWideValueStaysPendingOnTheNodesWhichHaveNotRestartedYet() {
        storedForCluster(entry(null, "cluster_wide", CHANGED_AT));
        when(configurationService.configurationStates()).thenReturn(Map.of(
                "restarted", appliedAt("restarted", AFTER_CHANGE),
                "not-restarted", appliedAt("not-restarted", BEFORE_CHANGE)));

        final var pending = pendingRestarts.pendingParameters(NodeType.DATANODE, List.of("restarted", "not-restarted"));

        assertThat(pending.get("restarted")).isEmpty();
        assertThat(pending.get("not-restarted")).containsExactly("cluster_wide");
    }

    @Test
    void nodeWhichNeverAppliedItsConfigurationHasEveryChangePending() {
        storedForCluster(entry(null, "cluster_wide", CHANGED_AT));
        storedForNode("node-1", entry("node-1", "per_node", CHANGED_AT));
        when(configurationService.configurationStates()).thenReturn(Map.of());

        assertThat(pendingRestarts.pendingParameters(NodeType.DATANODE, List.of("node-1")).get("node-1"))
                .containsExactly("cluster_wide", "per_node");
    }

    @Test
    void ignoresValuesWhichNeedNoRestartOrHaveNeverBeenChanged() {
        storedForNode("node-1",
                entry("node-1", "applied_live", CHANGED_AT),
                // Only reported by the node, never set through the API
                entry("node-1", "per_node", null));
        when(configurationService.configurationStates()).thenReturn(Map.of());

        assertThat(pendingRestarts.pendingParameters(NodeType.DATANODE, List.of("node-1")).get("node-1")).isEmpty();
    }

    @Test
    void ignoresStoredValuesOfParametersWhichAreNoLongerRegistered() {
        storedForNode("node-1", entry("node-1", "removed_from_the_catalog", CHANGED_AT));
        when(configurationService.configurationStates()).thenReturn(Map.of());

        assertThat(pendingRestarts.pendingParameters(NodeType.DATANODE, List.of("node-1")).get("node-1")).isEmpty();
    }

    @Test
    void nodeIsRestartingBetweenLoadingItsConfigurationAndItsProcessBecomingAvailable() {
        when(configurationService.configurationStates()).thenReturn(Map.of(
                "restarting", new NodeConfigurationState(null, "restarting", AFTER_CHANGE, BEFORE_CHANGE, null),
                "first-start", new NodeConfigurationState(null, "first-start", AFTER_CHANGE, null, null),
                "done", new NodeConfigurationState(null, "done", AFTER_CHANGE, AFTER_CHANGE, null)));

        assertThat(pendingRestarts.restartingNodes(NodeType.DATANODE, List.of("restarting", "first-start", "done", "unknown")))
                .containsExactly("first-start", "restarting");
        assertThat(pendingRestarts.isRestarting(NodeType.DATANODE, "done")).isFalse();
    }

    @Test
    void nodeWhichTakesTooLongToStartNoLongerCountsAsRestarting() {
        final Instant longAgo = NOW.minus(PendingRestarts.RESTART_TIMEOUT).minusSeconds(1);
        when(configurationService.configurationStates()).thenReturn(Map.of(
                "stuck", new NodeConfigurationState(null, "stuck", longAgo, longAgo.minusSeconds(60), null)));

        assertThat(pendingRestarts.isRestarting(NodeType.DATANODE, "stuck")).isFalse();
    }
}
