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
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the transaction log truncation floor handling in
 * {@link OpAmpService#handleMessage}: a collector whose last processed sequence lies below the
 * oldest retained marker gets a full config recompute because the markers it missed may have been
 * purged.
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
    void collectorBehindFloorGetsForcedConfigRecompute() {
        stubPreviousState(5L); // floor 101: everything up to seq 100 may have been purged
        stubNoUnprocessedMarkers(5L);
        when(fleetTransactionLogService.lowestRetainedSeq()).thenReturn(OptionalLong.of(101L));

        final ServerToAgent response = opAmpService.handleMessage(message(SEQUENCE_NUM, 0L), AUTH);

        assertThat(response.hasRemoteConfig()).isTrue();
        // hash = purge boundary, so the collector's APPLIED report lifts it past the purged range
        assertThat(response.getRemoteConfig().getConfigHash().toStringUtf8()).isEqualTo("100");
    }

    @Test
    void collectorAtFloorMinusOneGetsNoForcedRecompute() {
        // lastProcessedTxnSeq == floor - 1: the collector has processed the entire purged prefix
        stubPreviousState(100L);
        stubNoUnprocessedMarkers(100L);
        when(fleetTransactionLogService.lowestRetainedSeq()).thenReturn(OptionalLong.of(101L));

        final ServerToAgent response = opAmpService.handleMessage(message(SEQUENCE_NUM, 0L), AUTH);

        assertThat(response.hasRemoteConfig()).isFalse();
    }

    @Test
    void forcedRecomputeDoesNotLoopOnceBoundaryIsAcknowledged() {
        // The collector acknowledges the boundary hash from a previous forced recompute. Its
        // persisted cursor is still stale, but the APPLIED status must win and suppress a re-send.
        stubPreviousState(5L);
        stubNoUnprocessedMarkers(100L);
        when(fleetTransactionLogService.lowestRetainedSeq()).thenReturn(OptionalLong.of(101L));

        final var applied = Opamp.RemoteConfigStatus.newBuilder()
                .setStatus(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLIED)
                .setLastRemoteConfigHash(ByteString.copyFromUtf8("100"))
                .build();
        final var message = message(SEQUENCE_NUM, Opamp.AgentCapabilities.AgentCapabilities_ReportsRemoteConfig_VALUE)
                .toBuilder().setRemoteConfigStatus(applied).build();

        final ServerToAgent response = opAmpService.handleMessage(message, AUTH);

        assertThat(response.hasRemoteConfig()).isFalse();
    }

    @Test
    @SuppressWarnings("MustBeClosedChecker") // stubbing streamAllByFleet on a mock opens no resource
    void forcedRecomputePreservesRetainedFleetReassignment() {
        stubPreviousState(5L);
        // A FLEET_REASSIGNED marker (seq 150) survived the purge and coalesces alongside the floor
        when(fleetTransactionLogService.getUnprocessedMarkers(FLEET_ID, INSTANCE_UID, 5L)).thenReturn(List.of());
        when(fleetTransactionLogService.coalesce(any()))
                .thenReturn(new CoalescedActions(true, false, "fleet-B", false, false, 150L));
        when(fleetTransactionLogService.lowestRetainedSeq()).thenReturn(OptionalLong.of(101L));
        when(sourceService.streamAllByFleet("fleet-B")).thenReturn(Stream.empty());

        final ServerToAgent response = opAmpService.handleMessage(message(SEQUENCE_NUM, 0L), AUTH);

        assertThat(response.hasRemoteConfig()).isTrue();
        assertThat(response.getRemoteConfig().getConfigHash().toStringUtf8()).isEqualTo("150");
        verify(collectorInstanceService).updateCurrentFleet(INSTANCE_UID, "fleet-B");
    }

    @Test
    void emptyLogMeansNothingWasEverPurgedAndForcesNothing() {
        stubPreviousState(0L);
        stubNoUnprocessedMarkers(0L);
        when(fleetTransactionLogService.lowestRetainedSeq()).thenReturn(OptionalLong.empty());

        final ServerToAgent response = opAmpService.handleMessage(message(SEQUENCE_NUM, 0L), AUTH);

        assertThat(response.hasRemoteConfig()).isFalse();
        verify(sourceService, never()).streamAllByFleet(any());
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

    private void stubNoUnprocessedMarkers(long lastProcessedTxnSeq) {
        when(fleetTransactionLogService.getUnprocessedMarkers(FLEET_ID, INSTANCE_UID, lastProcessedTxnSeq))
                .thenReturn(List.of());
        when(fleetTransactionLogService.coalesce(List.of())).thenReturn(CoalescedActions.empty(0L));
    }

}
