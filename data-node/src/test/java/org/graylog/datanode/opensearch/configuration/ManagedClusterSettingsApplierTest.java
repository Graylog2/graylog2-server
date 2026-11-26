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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.configuration.managed.ManagedConfigurationApplier;
import org.graylog2.configuration.managed.ManagedConfigurationChangedEvent;
import org.graylog2.configuration.managed.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.GetClusterSettingsRequest;
import org.opensearch.client.opensearch.cluster.GetClusterSettingsResponse;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.opensearch.cluster.PutClusterSettingsRequest;
import org.opensearch.client.opensearch.cluster.PutClusterSettingsResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
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

    @Test
    void writesStoredValueOverAPersistentSetting() throws Exception {
        // Guards the test below: with this setup, a write does reach the cluster
        final OpenSearchClusterClient cluster = clusterWithPersistentSettings(Map.of(SHARDS, "3000"));
        when(managedConfigurationApplier.liveValues()).thenReturn(Map.of(SHARDS, "2000"));

        becomeAvailable();

        final ArgumentCaptor<PutClusterSettingsRequest> request = ArgumentCaptor.forClass(PutClusterSettingsRequest.class);
        verify(cluster).putSettings(request.capture());
        assertThat(request.getValue().persistent()).containsOnlyKeys(SHARDS);
    }

    /**
     * The database is the only source of the live settings once a value has been stored for them. That holds once the settings are managed through the web interface, but not on the first start after an
     * upgrade: the database is empty then, while the cluster may well carry persistent values which admins set
     * through the cluster settings API, before this feature existed. An empty database doesn't mean "reset", it
     * means "never configured here". Resetting those values silently undoes deliberate tuning, on every cluster,
     * without the admin doing anything but upgrading.
     * <p>
     * A fix has to tell these cases apart, e.g. by adopting existing persistent values into the database on the
     * first run (as is done for the values of the configuration files), or by only resetting values which this
     * applier wrote itself.
     */
    @Test
    void keepsPersistentSettingsMadeBeforeTheUpgrade() throws Exception {
        // A cluster upgraded to this version: an admin raised the shard limit through the cluster settings API
        // earlier, and nothing has been stored through the web interface yet
        final OpenSearchClusterClient cluster = clusterWithPersistentSettings(Map.of(SHARDS, "3000"));
        when(managedConfigurationApplier.liveValues()).thenReturn(Map.of());
        // Nothing has ever been stored for the setting, so the value can be adopted
        when(managedConfigurationApplier.adoptLiveValue(SHARDS, "3000")).thenReturn(true);

        // The first data node to become available after the upgrade applies the (empty) stored settings
        becomeAvailable();

        // Resetting would PUT {"cluster.max_shards_per_node": null} here. That drops the limit back to
        // the default of 1000, so as soon as the cluster holds more shards than that, creating indices fails
        // ("this action would add [N] total shards, but this cluster currently has [M]/[1000]"), index rotation
        // stops and ingestion stalls. Nothing in the web interface hints at the cause: the setting shows no stored
        // value, just as before the upgrade.
        verify(cluster, never()).putSettings(any(PutClusterSettingsRequest.class));
    }

    @Test
    void resetsASettingWhoseStoredValueHasBeenRemoved() throws Exception {
        final OpenSearchClusterClient cluster = clusterWithPersistentSettings(Map.of(SHARDS, "3000"));
        when(managedConfigurationApplier.liveValues()).thenReturn(Map.of());
        // A value has been stored and removed again, so it isn't adopted any more
        when(managedConfigurationApplier.adoptLiveValue(SHARDS, "3000")).thenReturn(false);

        becomeAvailable();

        final ArgumentCaptor<PutClusterSettingsRequest> request = ArgumentCaptor.forClass(PutClusterSettingsRequest.class);
        verify(cluster).putSettings(request.capture());
        assertThat(request.getValue().persistent()).containsOnlyKeys(SHARDS);
        assertThat(request.getValue().persistent().get(SHARDS).toJson()).isEqualTo(JsonValue.NULL);
    }

    @Test
    void appliesTheOtherSettingsWhenOpensearchRefusesOne() throws Exception {
        final OpenSearchClusterClient cluster = clusterWithPersistentSettings(Map.of());
        when(managedConfigurationApplier.liveValues()).thenReturn(Map.of(LOW, "95%", SHARDS, "2000"));
        refuse(cluster, LOW, "low disk watermark [95%] more than high disk watermark [90%]");

        becomeAvailable();

        verify(cluster, times(2)).putSettings(any(PutClusterSettingsRequest.class));
        final ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
        verify(managedConfigurationApplier).recordLiveSettingsResult(error.capture());
        // The reason opensearch gives is wrapped by the client, and has to be found among the causes
        assertThat(error.getValue())
                .contains(LOW)
                .contains("low disk watermark [95%] more than high disk watermark [90%]")
                .doesNotContain(SHARDS);
    }

    @Test
    void retriesOnHealthChecksUntilAnAttemptSucceeds() throws Exception {
        final OpenSearchClusterClient cluster = clusterWithPersistentSettings(Map.of());
        when(managedConfigurationApplier.liveValues()).thenReturn(Map.of(SHARDS, "2000"));
        refuse(cluster, SHARDS, "no cluster manager");
        final var applier = new ManagedClusterSettingsApplier(process, managedConfigurationApplier, new EventBus());

        applier.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);
        doReturn(PutClusterSettingsResponse.of(b -> b.acknowledged(true).persistent(Map.of()).transient_(Map.of())))
                .when(cluster).putSettings(any(PutClusterSettingsRequest.class));
        applier.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.AVAILABLE, OpensearchState.AVAILABLE);
        // Succeeded, so further health checks leave the settings alone
        applier.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.AVAILABLE, OpensearchState.AVAILABLE);

        verify(cluster, times(2)).putSettings(any(PutClusterSettingsRequest.class));
        final var inOrder = inOrder(managedConfigurationApplier);
        inOrder.verify(managedConfigurationApplier).recordLiveSettingsResult(contains("no cluster manager"));
        inOrder.verify(managedConfigurationApplier).recordLiveSettingsResult(isNull());
    }

    @Test
    void describesTheCausesOfAnException() {
        final var exception = new RuntimeException("Couldn't update cluster setting",
                new IllegalStateException("illegal_argument_exception", new IllegalArgumentException("the reason")));

        assertThat(ManagedClusterSettingsApplier.describe(exception))
                .isEqualTo("Couldn't update cluster setting: illegal_argument_exception: the reason");
    }

    private static void refuse(OpenSearchClusterClient cluster, String name, String reason) throws Exception {
        doAnswer(invocation -> {
            final PutClusterSettingsRequest request = invocation.getArgument(0);
            if (request.persistent().containsKey(name)) {
                throw new IllegalStateException("request failed", new IllegalArgumentException(reason));
            }
            return PutClusterSettingsResponse.of(b -> b.acknowledged(true).persistent(Map.of()).transient_(Map.of()));
        }).when(cluster).putSettings(any(PutClusterSettingsRequest.class));
    }

    private OpenSearchClusterClient clusterWithPersistentSettings(Map<String, String> persistent) throws Exception {
        final Map<String, JsonData> settings = new HashMap<>();
        persistent.forEach((name, value) -> settings.put(name, JsonData.of(Json.createValue(value))));
        final OpenSearchClusterClient cluster = mock(OpenSearchClusterClient.class, CALLS_REAL_METHODS);
        doReturn(GetClusterSettingsResponse.of(b -> b.persistent(settings).transient_(Map.of()).defaults(Map.of())))
                .when(cluster).getSettings(any(GetClusterSettingsRequest.class));
        doReturn(PutClusterSettingsResponse.of(b -> b.acknowledged(true).persistent(Map.of()).transient_(Map.of())))
                .when(cluster).putSettings(any(PutClusterSettingsRequest.class));
        final OpenSearchClient sync = mock(OpenSearchClient.class);
        when(sync.cluster()).thenReturn(cluster);
        final var client = new OfficialOpensearchClient(sync, mock(OpenSearchAsyncClient.class), new ObjectMapper());
        when(process.openSearchClient()).thenReturn(Optional.of(client));
        return cluster;
    }

    private void becomeAvailable() {
        new ManagedClusterSettingsApplier(process, managedConfigurationApplier, new EventBus())
                .transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);
    }
}
