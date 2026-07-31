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
import org.graylog.collectors.CollectorCaService;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorInstanceService.MinimalCollectorInstanceDTO;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog.collectors.opamp.auth.AgentTokenService;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.security.pki.CertificateService;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for how {@link OpAmpService#handleMessage} maps the OpAMP {@code ComponentHealth} payload
 * into the {@link CollectorInstanceReport} that gets persisted. The message uses a deliberately
 * non-consecutive sequence number, so the handler takes the ReportFullState short path and the
 * test needs no transaction-log or config stubbing — the health extraction under test happens
 * before that branch either way.
 */
@ExtendWith(MockitoExtension.class)
class OpAmpServiceHealthReportTest {

    private static final UUID INSTANCE_UUID = UUID.randomUUID();
    private static final String INSTANCE_UID = INSTANCE_UUID.toString();
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
    void setUp() {
        lenient().when(clusterIdService.getString()).thenReturn("clusterId");
        // Previous seq 0 vs message seq 10: non-consecutive on purpose, see class javadoc.
        when(collectorInstanceService.updateFromReport(any())).thenReturn(
                new MinimalCollectorInstanceDTO("id-1", "fleet-A", 0L, 0L, null));
        opAmpService = new OpAmpService(enrollmentTokenService, agentTokenService, collectorCaService,
                certificateService, collectorInstanceService, collectorsConfigService, clusterIdService,
                fleetTransactionLogService, sourceService);
    }

    @Test
    void mapsTopLevelHealthFieldsIntoInstanceReport() {
        final long startNanos = 1_700_000_000_000_000_000L;
        final long statusNanos = 1_700_000_100_123_456_789L;
        final var health = Opamp.ComponentHealth.newBuilder()
                .setHealthy(false)
                .setLastError("connection refused")
                .setStatus("StatusRecoverableError")
                .setStartTimeUnixNano(startNanos)
                .setStatusTimeUnixNano(statusNanos)
                .build();

        opAmpService.handleMessage(messageWithHealth(health), AUTH);

        assertThat(capturedReport().health()).hasValueSatisfying(mapped -> {
            assertThat(mapped.healthy()).isFalse();
            assertThat(mapped.lastError()).contains("connection refused");
            assertThat(mapped.status()).contains("StatusRecoverableError");
            assertThat(mapped.startTime()).contains(Instant.ofEpochSecond(0, startNanos));
            assertThat(mapped.statusTime()).contains(Instant.ofEpochSecond(0, statusNanos));
            assertThat(mapped.components()).isEmpty();
        });
    }

    @Test
    void mapsComponentTreeRecursively() {
        final var pipeline = Opamp.ComponentHealth.newBuilder()
                .setHealthy(false)
                .setStatus("StatusPermanentError")
                .setLastError("exporter failed")
                .build();
        final var collector = Opamp.ComponentHealth.newBuilder()
                .setHealthy(false)
                .setStatus("StatusRecoverableError")
                .putComponentHealthMap("pipeline:logs/abc", pipeline)
                .build();
        final var root = Opamp.ComponentHealth.newBuilder()
                .setHealthy(true)
                .putComponentHealthMap("collector", collector)
                .build();

        opAmpService.handleMessage(messageWithHealth(root), AUTH);

        assertThat(capturedReport().health()).hasValueSatisfying(mapped -> {
            assertThat(mapped.healthy()).isTrue();
            assertThat(mapped.components()).containsOnlyKeys("collector");

            final var mappedCollector = mapped.components().get("collector");
            assertThat(mappedCollector.healthy()).isFalse();
            assertThat(mappedCollector.status()).contains("StatusRecoverableError");
            assertThat(mappedCollector.components()).containsOnlyKeys("pipeline:logs/abc");

            final var mappedPipeline = mappedCollector.components().get("pipeline:logs/abc");
            assertThat(mappedPipeline.healthy()).isFalse();
            assertThat(mappedPipeline.status()).contains("StatusPermanentError");
            assertThat(mappedPipeline.lastError()).contains("exporter failed");
            assertThat(mappedPipeline.components()).isEmpty();
        });
    }

    @Test
    void normalizesProto3DefaultsToAbsent() {
        // Proto3 defaults: empty strings and 0 timestamps. Per the OpAMP spec, a start time of 0
        // means "not running" — all of these must map to absent, not to empty/zero values.
        final var health = Opamp.ComponentHealth.newBuilder()
                .setHealthy(true)
                .build();

        opAmpService.handleMessage(messageWithHealth(health), AUTH);

        assertThat(capturedReport().health()).hasValueSatisfying(mapped -> {
            assertThat(mapped.healthy()).isTrue();
            assertThat(mapped.lastError()).isEmpty();
            assertThat(mapped.status()).isEmpty();
            assertThat(mapped.startTime()).isEmpty();
            assertThat(mapped.statusTime()).isEmpty();
            assertThat(mapped.components()).isEmpty();
        });
    }

    @Test
    void normalizesBlankStringsToAbsent() {
        final var health = Opamp.ComponentHealth.newBuilder()
                .setHealthy(true)
                .setLastError("   ")
                .setStatus("   ")
                .build();

        opAmpService.handleMessage(messageWithHealth(health), AUTH);

        assertThat(capturedReport().health()).hasValueSatisfying(mapped -> {
            assertThat(mapped.lastError()).isEmpty();
            assertThat(mapped.status()).isEmpty();
        });
    }

    @Test
    void leavesHealthEmptyWhenMessageCarriesNone() {
        // ReportsHealth capability set, but no health field: OpAMP compression — the agent omits
        // unchanged health. The report must not carry a health value (the stored one is kept).
        final var message = message(Opamp.AgentCapabilities.AgentCapabilities_ReportsHealth_VALUE).build();

        opAmpService.handleMessage(message, AUTH);

        assertThat(capturedReport().health()).isEmpty();
    }

    @Test
    void ignoresHealthWithoutReportsHealthCapability() {
        // Health is gated by the ReportsHealth capability; a payload without the capability bit
        // must be ignored.
        final var message = message(0L)
                .setHealth(Opamp.ComponentHealth.newBuilder().setHealthy(true))
                .build();

        opAmpService.handleMessage(message, AUTH);

        assertThat(capturedReport().health()).isEmpty();
    }

    private CollectorInstanceReport capturedReport() {
        final var captor = ArgumentCaptor.forClass(CollectorInstanceReport.class);
        verify(collectorInstanceService).updateFromReport(captor.capture());
        return captor.getValue();
    }

    private static AgentToServer messageWithHealth(Opamp.ComponentHealth health) {
        return message(Opamp.AgentCapabilities.AgentCapabilities_ReportsHealth_VALUE)
                .setHealth(health)
                .build();
    }

    private static AgentToServer.Builder message(long capabilities) {
        final ByteBuffer uid = ByteBuffer.allocate(16)
                .putLong(INSTANCE_UUID.getMostSignificantBits())
                .putLong(INSTANCE_UUID.getLeastSignificantBits());
        return AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uid.array()))
                .setSequenceNum(SEQUENCE_NUM)
                .setCapabilities(capabilities);
    }
}
