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
package org.graylog.collectors.opamp;

import com.google.protobuf.ByteString;
import opamp.proto.Opamp;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.ServerToAgent;
import org.graylog.collectors.CollectorCaService;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorInstanceService.MinimalCollectorInstanceDTO;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.CoalescedActions;
import org.graylog.collectors.opamp.auth.AgentTokenService;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for how {@link OpAmpService#handleMessage} handles transaction log truncation: the
 * decision whether a collector's cursor lies below the purged range is made by
 * {@link FleetTransactionLogService#coalesce(List, long)} (covered by its own tests) — here we
 * verify that the server passes the right cursor and acts correctly on a forced recompute.
 */
@ExtendWith(MockitoExtension.class)
class OpAmpServiceTransactionLogTruncationTest {

    private static final UUID INSTANCE_UUID = UUID.randomUUID();
    private static final String INSTANCE_UID = INSTANCE_UUID.toString();
    private static final String FLEET_ID = "fleet-A";
    private static final long SEQUENCE_NUM = 10L;

    private static final OpAmpAuthContext.Identified AUTH =
            new OpAmpAuthContext.Identified(INSTANCE_UID, OpAmpAuthContext.Transport.HTTP);

    @Mock
    private EnrollmentTokenService enrollmentTokenService;
    @Mock
    private AgentTokenService agentTokenService;
    @Mock
    private CollectorCaService collectorCaService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private CollectorInstanceService collectorInstanceService;
    @Mock
    private CollectorsConfigService collectorsConfigService;
    @Mock
    private ClusterIdService clusterIdService;
    @Mock
    private FleetTransactionLogService fleetTransactionLogService;
    @Mock
    private SourceService sourceService;

    private OpAmpService opAmpService;

    @BeforeEach
    @SuppressWarnings("MustBeClosedChecker") // stubbing streamAllByFleet on a mock opens no resource
    void setUp() {
        lenient().when(clusterIdService.getString()).thenReturn("clusterId");
        // the exporter config is built on every identified exchange, forced recompute or not
        final var caCert = mock(CertificateEntry.class);
        lenient().when(caCert.certificate()).thenReturn("ca-pem");
        lenient().when(collectorCaService.getCaCert()).thenReturn(caCert);
        lenient().when(collectorsConfigService.getOrDefault()).thenReturn(CollectorsConfig.createDefault("localhost"));
        lenient().when(sourceService.streamAllByFleet(FLEET_ID)).thenReturn(Stream.empty());
        opAmpService = new OpAmpService(enrollmentTokenService, agentTokenService, collectorCaService,
                certificateService, collectorInstanceService, collectorsConfigService, clusterIdService,
                fleetTransactionLogService, sourceService);
    }

    @Test
    void forcedRecomputeIsDeliveredAsRemoteConfigWithThePurgedRangeAcknowledged() {
        stubPreviousState(5L);
        stubMarkers(5L, List.of());
        // markers up to seq 100 were purged: the service forces a recompute acknowledging them
        when(fleetTransactionLogService.coalesce(List.of(), 5L))
                .thenReturn(CoalescedActions.empty(0L).withForcedRecompute(100L));

        final ServerToAgent response = opAmpService.handleMessage(message(SEQUENCE_NUM, 0L), AUTH);

        assertThat(response.hasRemoteConfig()).isTrue();
        // hash = purged range, so the collector's APPLIED report lifts its cursor past it
        assertThat(response.getRemoteConfig().getConfigHash().toStringUtf8()).isEqualTo("100");
    }

    @Test
    void noConfigIsSentWithoutMarkersOrForcedRecompute() {
        stubPreviousState(100L);
        stubMarkers(100L, List.of());
        when(fleetTransactionLogService.coalesce(List.of(), 100L)).thenReturn(CoalescedActions.empty(0L));

        final ServerToAgent response = opAmpService.handleMessage(message(SEQUENCE_NUM, 0L), AUTH);

        assertThat(response.hasRemoteConfig()).isFalse();
        verify(sourceService, never()).streamAllByFleet(any());
    }

    @Test
    void appliedStatusAdvancesTheCursorPassedToCoalesce() {
        // The collector acknowledges hash "100" from a previous forced recompute. Even though its
        // persisted cursor is stale (5), the APPLIED status must win — this is what terminates the
        // forcing after one exchange instead of looping.
        stubPreviousState(5L);
        stubMarkers(100L, List.of());
        when(fleetTransactionLogService.coalesce(List.of(), 100L)).thenReturn(CoalescedActions.empty(0L));

        final var applied = Opamp.RemoteConfigStatus.newBuilder()
                .setStatus(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLIED)
                .setLastRemoteConfigHash(ByteString.copyFromUtf8("100"))
                .build();
        final var message = message(SEQUENCE_NUM, Opamp.AgentCapabilities.AgentCapabilities_ReportsRemoteConfig_VALUE)
                .toBuilder().setRemoteConfigStatus(applied).build();

        final ServerToAgent response = opAmpService.handleMessage(message, AUTH);

        assertThat(response.hasRemoteConfig()).isFalse();
        verify(fleetTransactionLogService).coalesce(List.of(), 100L);
    }

    @Test
    @SuppressWarnings("MustBeClosedChecker") // stubbing streamAllByFleet on a mock opens no resource
    void forcedRecomputePreservesRetainedFleetReassignment() {
        stubPreviousState(5L);
        stubMarkers(5L, List.of());
        // a FLEET_REASSIGNED marker (seq 150) survived the purge and coalesces alongside the forcing
        when(fleetTransactionLogService.coalesce(List.of(), 5L))
                .thenReturn(new CoalescedActions(true, true, "fleet-B", false, false, 150L));
        when(sourceService.streamAllByFleet("fleet-B")).thenReturn(Stream.empty());

        final ServerToAgent response = opAmpService.handleMessage(message(SEQUENCE_NUM, 0L), AUTH);

        assertThat(response.hasRemoteConfig()).isTrue();
        assertThat(response.getRemoteConfig().getConfigHash().toStringUtf8()).isEqualTo("150");
        verify(collectorInstanceService).updateCurrentFleet(INSTANCE_UID, "fleet-B");
    }

    private static AgentToServer message(long sequenceNum, long capabilities) {
        final ByteBuffer uid = ByteBuffer.allocate(16)
                .putLong(INSTANCE_UUID.getMostSignificantBits())
                .putLong(INSTANCE_UUID.getLeastSignificantBits());
        return AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uid.array()))
                .setSequenceNum(sequenceNum)
                .setCapabilities(capabilities)
                .build();
    }

    private void stubPreviousState(long lastProcessedTxnSeq) {
        when(collectorInstanceService.updateFromReport(any())).thenReturn(
                new MinimalCollectorInstanceDTO("id-1", FLEET_ID, SEQUENCE_NUM - 1, lastProcessedTxnSeq, null));
    }

    private void stubMarkers(long lastProcessedTxnSeq, List<org.graylog.collectors.db.TransactionMarker> markers) {
        when(fleetTransactionLogService.getUnprocessedMarkers(eq(FLEET_ID), eq(INSTANCE_UID), eq(lastProcessedTxnSeq)))
                .thenReturn(markers);
    }
}
