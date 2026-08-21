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
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.opamp.auth.AgentTokenService;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.security.pki.CertificateService;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpAmpServiceAuthCheckTest {

    private static final OpAmpAuthContext.Transport TRANSPORT = OpAmpAuthContext.Transport.HTTP;

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
        opAmpService = new OpAmpService(enrollmentTokenService, agentTokenService, collectorCaService, certificateService,
                collectorInstanceService, collectorsConfigService, clusterIdService, fleetTransactionLogService, sourceService);
    }

    @Test
    void answersAuthCheckWithEnrollmentTokenWithoutSideEffects() {
        final UUID instanceUuid = UUID.randomUUID();
        final AgentToServer message = buildAuthCheckMessage(instanceUuid);
        final var auth = new OpAmpAuthContext.Enrollment(enrollmentToken(), TRANSPORT);

        final var response = opAmpService.handleMessage(message, auth);

        assertThat(response.hasErrorResponse()).isFalse();
        assertThat(response.getInstanceUid()).isEqualTo(message.getInstanceUid());
        assertThat(response.getCustomCapabilities().getCapabilitiesList())
                .containsExactly(OpAmpConstants.AUTH_CHECK_CUSTOM_CAPABILITY);
        verifyNoInteractions(enrollmentTokenService, collectorInstanceService);
    }

    @Test
    void answersAuthCheckWithAgentTokenWithoutSideEffects() {
        final UUID instanceUuid = UUID.randomUUID();
        final AgentToServer message = buildAuthCheckMessage(instanceUuid);
        final var auth = new OpAmpAuthContext.Identified(instanceUuid.toString(), TRANSPORT);

        final var response = opAmpService.handleMessage(message, auth);

        assertThat(response.hasErrorResponse()).isFalse();
        assertThat(response.getInstanceUid()).isEqualTo(message.getInstanceUid());
        assertThat(response.getCustomCapabilities().getCapabilitiesList())
                .containsExactly(OpAmpConstants.AUTH_CHECK_CUSTOM_CAPABILITY);
        verifyNoInteractions(enrollmentTokenService, collectorInstanceService);
    }

    @Test
    void announcesCustomCapabilityOnFirstMessageOfSession() {
        final UUID instanceUuid = UUID.randomUUID();
        // A regular identified message (no custom message) with sequence number 0, i.e. the
        // first message of a session.
        final AgentToServer message = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes(instanceUuid)))
                .build();
        final var auth = new OpAmpAuthContext.Identified(instanceUuid.toString(), TRANSPORT);

        // Previous seq 0 vs message seq 0: non-consecutive, so the handler takes the
        // ReportFullState short path and needs no transaction-log or config stubbing.
        when(collectorInstanceService.updateFromReport(any())).thenReturn(
                new MinimalCollectorInstanceDTO("id-1", "fleet-A", 0L, 0L, null));

        final var response = opAmpService.handleMessage(message, auth);

        assertThat(response.getCustomCapabilities().getCapabilitiesList())
                .containsExactly(OpAmpConstants.AUTH_CHECK_CUSTOM_CAPABILITY);
    }

    @Test
    void doesNotAnnounceCustomCapabilityOnSubsequentMessages() {
        final UUID instanceUuid = UUID.randomUUID();
        final AgentToServer message = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes(instanceUuid)))
                .setSequenceNum(10L)
                .build();
        final var auth = new OpAmpAuthContext.Identified(instanceUuid.toString(), TRANSPORT);

        when(collectorInstanceService.updateFromReport(any())).thenReturn(
                new MinimalCollectorInstanceDTO("id-1", "fleet-A", 0L, 0L, null));

        final var response = opAmpService.handleMessage(message, auth);

        assertThat(response.hasCustomCapabilities()).isFalse();
    }

    @Test
    void ignoresCustomMessageWithUnknownCapability() {
        final UUID instanceUuid = UUID.randomUUID();
        final AgentToServer message = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes(instanceUuid)))
                .setCustomMessage(Opamp.CustomMessage.newBuilder()
                        .setCapability("org.example.unknown")
                        .setType(OpAmpConstants.AUTH_CHECK_MESSAGE_TYPE))
                .build();
        final var auth = new OpAmpAuthContext.Enrollment(enrollmentToken(), TRANSPORT);

        final var response = opAmpService.handleMessage(message, auth);

        // Not an auth-check, so the regular enrollment handling applies (which requires a CSR).
        assertThat(response.hasErrorResponse()).isTrue();
        assertThat(response.getErrorResponse().getErrorMessage()).contains("Missing CSR");
    }

    private AgentToServer buildAuthCheckMessage(UUID instanceUuid) {
        return AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes(instanceUuid)))
                .setCustomCapabilities(Opamp.CustomCapabilities.newBuilder()
                        .addCapabilities(OpAmpConstants.AUTH_CHECK_CUSTOM_CAPABILITY))
                .setCustomMessage(Opamp.CustomMessage.newBuilder()
                        .setCapability(OpAmpConstants.AUTH_CHECK_CUSTOM_CAPABILITY)
                        .setType(OpAmpConstants.AUTH_CHECK_MESSAGE_TYPE))
                .build();
    }

    private EnrollmentTokenDTO enrollmentToken() {
        return new EnrollmentTokenDTO("token-id", "test-token", "jti", "kid", "fleet-id",
                new EnrollmentTokenCreator("user-id", "admin"), Instant.now(), null, 0, null);
    }

    private static byte[] uuidBytes(UUID uuid) {
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
