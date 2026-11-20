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
package org.graylog.datanode.opensearch.configuration;

import com.google.common.eventbus.EventBus;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog2.configuration.managed.ManagedConfigurationApplier;
import org.graylog2.configuration.managed.ManagedConfigurationChangedEvent;
import org.graylog2.configuration.managed.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagedClusterSettingsApplierTest {
    private static final String LOW = "cluster.routing.allocation.disk.watermark.low";
    private static final String SHARDS = "cluster.max_shards_per_node";

    private final OpensearchProcess process = mock(OpensearchProcess.class);
    private final ManagedConfigurationApplier managedConfigurationApplier = mock(ManagedConfigurationApplier.class);
    private final EventBus eventBus = new EventBus();

    @BeforeEach
    void setUp() {
        when(managedConfigurationApplier.liveParameters()).thenReturn(Set.of(LOW, SHARDS));
        // Without a running process nothing is applied, which is enough to see whether applying is attempted
        when(process.openSearchClient()).thenReturn(Optional.empty());
        new ManagedClusterSettingsApplier(process, managedConfigurationApplier, eventBus);
    }

    @Test
    void writesStoredValuesWhichDifferFromThePersistentSettings() {
        final var changes = ManagedClusterSettingsApplier.changes(
                Map.of(LOW, "80%", SHARDS, "1000"),
                Set.of(LOW, SHARDS),
                Map.of(LOW, "85%", SHARDS, "1000"));

        assertThat(changes).containsExactly(Map.entry(LOW, "80%"));
    }

    @Test
    void resetsManagedSettingsWithoutAStoredValue() {
        final var changes = ManagedClusterSettingsApplier.changes(
                Map.of(),
                Set.of(LOW, SHARDS),
                Map.of(LOW, "80%", "cluster.routing.allocation.enable", "primaries"));

        final var expected = new HashMap<String, String>();
        expected.put(LOW, null);
        // Settings which aren't managed are left alone, and settings which aren't set don't need a reset
        assertThat(changes).isEqualTo(expected);
    }

    @Test
    void changesNothingWhenTheSettingsMatch() {
        assertThat(ManagedClusterSettingsApplier.changes(Map.of(LOW, " 80% "), Set.of(LOW), Map.of(LOW, "80%")))
                .isEmpty();
        assertThat(ManagedClusterSettingsApplier.changes(Map.of(), Set.of(LOW), Map.of())).isEmpty();
    }

    @Test
    void appliesWhenAClusterWideLiveSettingChanged() {
        eventBus.post(new ManagedConfigurationChangedEvent(NodeType.DATANODE, null, Set.of(LOW)));

        verify(process).openSearchClient();
    }

    @Test
    void ignoresChangesOfOtherParameters() {
        eventBus.post(new ManagedConfigurationChangedEvent(NodeType.DATANODE, null, Set.of("opensearch_heap")));
        eventBus.post(new ManagedConfigurationChangedEvent(NodeType.DATANODE, "node-1", Set.of(LOW)));
        eventBus.post(new ManagedConfigurationChangedEvent(NodeType.SERVER, null, Set.of(LOW)));

        verify(process, never()).openSearchClient();
    }

    @Test
    void appliesWhenOpensearchBecomesAvailable() {
        final var applier = new ManagedClusterSettingsApplier(process, managedConfigurationApplier, new EventBus());

        applier.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);
        // Health checks keep the process available, which mustn't apply the settings every few seconds
        applier.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.AVAILABLE, OpensearchState.AVAILABLE);

        verify(process, times(1)).openSearchClient();
    }
}
