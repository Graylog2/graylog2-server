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
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.opamp.auth.AgentTokenService;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.TestClocks;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OpAmpService}, covering the fresh / re-enroll / reject branches.
 */
@ExtendWith(MockitoExtension.class)
class OpAmpServiceHandleEnrollmentTest {

    private static final OpAmpAuthContext.Transport TRANSPORT = OpAmpAuthContext.Transport.HTTP;

    private static CertificateBuilder certBuilder;
    private static CertificateEntry issuerCert;

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

    @BeforeAll
    static void beforeAll() throws Exception {
        final var encryptedValueService = new EncryptedValueService("1234567890abcdef");
        certBuilder = new CertificateBuilder(encryptedValueService, "Graylog", TestClocks.fixedEpoch());
        final var caCert = certBuilder.createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(3650))
                .withId("100000000000000000000000");
        issuerCert = certBuilder.createIntermediateCa("Test Issuer", caCert, Duration.ofDays(1825))
                .withId("200000000000000000000000");
    }

    @BeforeEach
    void setUp() {
        lenient().when(clusterIdService.getString()).thenReturn("clusterId");
        lenient().when(certificateService.builder()).thenReturn(certBuilder);
        lenient().when(collectorCaService.getSigningCert()).thenReturn(issuerCert);
        lenient().when(collectorsConfigService.getOrDefault()).thenReturn(CollectorsConfig.createDefault("localhost"));

        opAmpService = new OpAmpService(enrollmentTokenService, agentTokenService, collectorCaService, certificateService,
                collectorInstanceService, collectorsConfigService, clusterIdService, fleetTransactionLogService, sourceService);
    }

    @Test
    void enrollsNewCollectorWhenNoExistingRecord() throws Exception {
        final KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final UUID instanceUuid = UUID.randomUUID();
        final AgentToServer message = buildEnrollmentMessage(instanceUuid, keyPair);
        final OpAmpAuthContext.Enrollment auth = buildAuth("new-token-id", "new-fleet-id");

        when(collectorInstanceService.findByInstanceUid(instanceUuid.toString())).thenReturn(Optional.empty());
        when(collectorInstanceService.enroll(any(), any(), any(), any()))
                .thenReturn(dummyInstance(instanceUuid.toString(), "new-fleet-id", "new-token-id"));

        final var response = opAmpService.handleMessage(message, auth);

        assertThat(response.hasErrorResponse()).isFalse();
        assertThat(response.hasConnectionSettings()).isTrue();
        verify(collectorInstanceService).enroll(eq(instanceUuid.toString()), eq("new-fleet-id"), any(IssuedCertificate.class), eq("new-token-id"));
        verify(collectorInstanceService, never()).reEnroll(any(), any(), any(), any());
        verify(enrollmentTokenService).incrementUsage("new-token-id");
        verify(enrollmentTokenService, never()).markUsed(any());
    }

    @Test
    void reEnrollsWhenExistingRecordKeyMatchesAndDifferentTokenIncrements() throws Exception {
        // Same keypair produces the same CSR pubkey → PoP passes.
        final KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final UUID instanceUuid = UUID.randomUUID();
        final AgentToServer message = buildEnrollmentMessage(instanceUuid, keyPair);
        final OpAmpAuthContext.Enrollment auth = buildAuth("new-token-id", "new-fleet-id");

        final CollectorInstanceDTO existing = existingRecordFor(instanceUuid.toString(), keyPair, "original-token-id", "original-fleet-id");
        when(collectorInstanceService.findByInstanceUid(instanceUuid.toString())).thenReturn(Optional.of(existing));
        when(collectorInstanceService.reEnroll(any(), any(), any(), any())).thenReturn(existing);

        final var response = opAmpService.handleMessage(message, auth);

        assertThat(response.hasErrorResponse()).isFalse();
        assertThat(response.hasConnectionSettings()).isTrue();
        verify(collectorInstanceService).reEnroll(eq(existing.id()), eq(existing.activeCertificateFingerprint()),
                any(IssuedCertificate.class), eq("new-token-id"));
        verify(collectorInstanceService, never()).enroll(any(), any(), any(), any());
        verify(enrollmentTokenService).incrementUsage("new-token-id");
        verify(enrollmentTokenService, never()).markUsed(any());
    }

    @Test
    void reEnrollsWhenExistingRecordKeyMatchesAndSameTokenSkipsIncrement() throws Exception {
        final KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final UUID instanceUuid = UUID.randomUUID();
        final AgentToServer message = buildEnrollmentMessage(instanceUuid, keyPair);
        final OpAmpAuthContext.Enrollment auth = buildAuth("same-token-id", "same-fleet-id");

        final CollectorInstanceDTO existing = existingRecordFor(instanceUuid.toString(), keyPair, "same-token-id", "same-fleet-id");
        when(collectorInstanceService.findByInstanceUid(instanceUuid.toString())).thenReturn(Optional.of(existing));
        when(collectorInstanceService.reEnroll(any(), any(), any(), any())).thenReturn(existing);

        final var response = opAmpService.handleMessage(message, auth);

        assertThat(response.hasErrorResponse()).isFalse();
        verify(collectorInstanceService).reEnroll(eq(existing.id()), eq(existing.activeCertificateFingerprint()),
                any(IssuedCertificate.class), eq("same-token-id"));
        verify(enrollmentTokenService, never()).incrementUsage(any());
        // The skipped increment must not freeze the token's last_used_at — it would look dormant
        // to operators even though a collector still relies on it for recovery.
        verify(enrollmentTokenService).markUsed("same-token-id");
    }

    @Test
    void rejectsReEnrollmentWhenKeyDoesNotMatchStoredCertificate() throws Exception {
        // Incoming CSR uses keyPair1; stored record cert is bound to keyPair2 → PoP mismatch.
        final KeyPair incomingKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final KeyPair storedKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final UUID instanceUuid = UUID.randomUUID();
        final AgentToServer message = buildEnrollmentMessage(instanceUuid, incomingKey);
        final OpAmpAuthContext.Enrollment auth = buildAuth("token-id", "fleet-id");

        final CollectorInstanceDTO existing = existingRecordFor(instanceUuid.toString(), storedKey, "original-token-id", "original-fleet-id");
        when(collectorInstanceService.findByInstanceUid(instanceUuid.toString())).thenReturn(Optional.of(existing));

        final var response = opAmpService.handleMessage(message, auth);

        assertThat(response.hasErrorResponse()).isTrue();
        assertThat(response.getErrorResponse().getErrorMessage()).isEqualTo("Enrollment rejected.");
        verify(collectorInstanceService, never()).enroll(any(), any(), any(), any());
        verify(collectorInstanceService, never()).reEnroll(any(), any(), any(), any());
        verify(enrollmentTokenService, never()).incrementUsage(any());
        verify(enrollmentTokenService, never()).markUsed(any());
    }

    @Test
    void returnsErrorWhenCsrIsMissing() {
        final AgentToServer message = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes(UUID.randomUUID())))
                .build();
        final OpAmpAuthContext.Enrollment auth = buildAuth("token-id", "fleet-id");

        final var response = opAmpService.handleMessage(message, auth);

        assertThat(response.hasErrorResponse()).isTrue();
        assertThat(response.getErrorResponse().getErrorMessage()).contains("Missing CSR");
        verify(collectorInstanceService, never()).enroll(any(), any(), any(), any());
        verify(collectorInstanceService, never()).reEnroll(any(), any(), any(), any());
        verify(enrollmentTokenService, never()).incrementUsage(any());
        verify(enrollmentTokenService, never()).markUsed(any());
    }

    // --- helpers ---

    private AgentToServer buildEnrollmentMessage(UUID instanceUuid, KeyPair keyPair) throws Exception {
        final byte[] csrPem = certBuilder.createCsr(keyPair, "agent-" + instanceUuid);
        return AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes(instanceUuid)))
                .setConnectionSettingsRequest(Opamp.ConnectionSettingsRequest.newBuilder()
                        .setOpamp(Opamp.OpAMPConnectionSettingsRequest.newBuilder()
                                .setCertificateRequest(Opamp.CertificateRequest.newBuilder()
                                        .setCsr(ByteString.copyFrom(csrPem)))))
                .build();
    }

    private OpAmpAuthContext.Enrollment buildAuth(String tokenId, String fleetId) {
        final EnrollmentTokenDTO dto = new EnrollmentTokenDTO(tokenId, "test-token", "jti", "kid", fleetId,
                new EnrollmentTokenCreator("user-id", "admin"), Instant.now(), null, 0, null);
        return new OpAmpAuthContext.Enrollment(dto, TRANSPORT);
    }

    private CollectorInstanceDTO existingRecordFor(String instanceUid, KeyPair keyPair, String tokenId, String fleetId) throws Exception {
        final byte[] csrPem = certBuilder.createCsr(keyPair, "agent-" + instanceUid);
        final var cert = certBuilder.signCsr(PemUtils.parseCsr(new String(csrPem, java.nio.charset.StandardCharsets.UTF_8)),
                issuerCert, instanceUid, Duration.ofDays(365));
        return CollectorInstanceDTO.builder()
                .id("507f1f77bcf86cd799439abc")
                .instanceUid(instanceUid)
                .messageSeqNum(0L)
                .capabilities(0L)
                .lastSeen(Instant.EPOCH)
                .fleetId(fleetId)
                .activeCertificateFingerprint(PemUtils.computeFingerprint(cert))
                .activeCertificatePem(PemUtils.toPem(cert))
                .activeCertificateExpiresAt(cert.getNotAfter().toInstant())
                .issuingCaId(issuerCert.id())
                .enrolledAt(Instant.EPOCH)
                .enrollmentTokenId(tokenId)
                .build();
    }

    private CollectorInstanceDTO dummyInstance(String instanceUid, String fleetId, String tokenId) {
        return CollectorInstanceDTO.builder()
                .id("507f1f77bcf86cd799439def")
                .instanceUid(instanceUid)
                .messageSeqNum(0L)
                .capabilities(0L)
                .lastSeen(Instant.EPOCH)
                .fleetId(fleetId)
                .activeCertificateFingerprint("sha256:0000000000000000000000000000000000000000000000000000000000000000")
                .activeCertificatePem("-----BEGIN CERTIFICATE-----\n-----END CERTIFICATE-----\n")
                .activeCertificateExpiresAt(Instant.EPOCH.plus(Duration.ofDays(365)))
                .issuingCaId(issuerCert.id())
                .enrolledAt(Instant.EPOCH)
                .enrollmentTokenId(tokenId)
                .build();
    }

    private static byte[] uuidBytes(UUID uuid) {
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
