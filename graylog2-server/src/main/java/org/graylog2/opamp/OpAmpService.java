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
package org.graylog2.opamp;

import com.google.protobuf.ByteString;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.ConnectionSettingsOffers;
import opamp.proto.Opamp.OpAMPConnectionSettings;
import opamp.proto.Opamp.ServerErrorResponse;
import opamp.proto.Opamp.ServerToAgent;
import opamp.proto.Opamp.TLSCertificate;
import org.graylog.security.certificates.CertificateEntry;
import org.graylog.security.certificates.CertificateService;
import org.graylog.security.certificates.PemUtils;
import org.graylog2.opamp.enrollment.EnrollmentTokenService;
import org.graylog2.opamp.transport.OpAmpAuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class OpAmpService {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpService.class);

    private final EnrollmentTokenService enrollmentTokenService;
    private final OpAmpAgentService agentService;
    private final CertificateService certificateService;

    @Inject
    public OpAmpService(EnrollmentTokenService enrollmentTokenService,
                        OpAmpAgentService agentService,
                        CertificateService certificateService) {
        this.enrollmentTokenService = enrollmentTokenService;
        this.agentService = agentService;
        this.certificateService = certificateService;
    }

    public Optional<OpAmpAuthContext> authenticate(String authHeader, URI effectiveExternalUri, OpAmpAuthContext.Transport transport) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        final String token = authHeader.substring(7);

        final String typ = extractTypHeader(token);
        if (typ == null) {
            LOG.warn("Token missing typ header");
            return Optional.empty();
        }

        return switch (typ) {
            case "enrollment+jwt" -> enrollmentTokenService.validateToken(token, effectiveExternalUri, transport)
                    .map(e -> e);
            case "agent+jwt" -> enrollmentTokenService.validateAgentToken(token, transport)
                    .map(i -> i);
            default -> {
                LOG.warn("Unknown token type: {}", typ);
                yield Optional.empty();
            }
        };
    }

    private String extractTypHeader(String token) {
        try {
            final String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            final String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            // Simple extraction - find "typ":"value"
            final int typIndex = headerJson.indexOf("\"typ\"");
            if (typIndex < 0) {
                return null;
            }
            final int colonIndex = headerJson.indexOf(':', typIndex);
            final int startQuote = headerJson.indexOf('"', colonIndex);
            final int endQuote = headerJson.indexOf('"', startQuote + 1);
            if (startQuote < 0 || endQuote < 0) {
                return null;
            }
            return headerJson.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            LOG.warn("Failed to extract typ header: {}", e.getMessage());
            return null;
        }
    }

    public ServerToAgent handleMessage(AgentToServer message, OpAmpAuthContext authContext) {
        return switch (authContext) {
            case OpAmpAuthContext.Enrollment e -> handleEnrollment(message, e);
            case OpAmpAuthContext.Identified i -> handleIdentifiedMessage(message, i);
        };
    }

    private ServerToAgent handleEnrollment(AgentToServer message, OpAmpAuthContext.Enrollment auth) {
        final String instanceUid = bytesToUuid(message.getInstanceUid().toByteArray()).toString();

        // 1. Reject if already enrolled
        if (agentService.existsByInstanceUid(instanceUid)) {
            LOG.warn("Rejecting enrollment: agent {} already enrolled", instanceUid);
            return ServerToAgent.newBuilder()
                    .setInstanceUid(message.getInstanceUid())
                    .setErrorResponse(ServerErrorResponse.newBuilder()
                            .setErrorMessage("Agent already enrolled"))
                    .build();
        }

        // 2. Extract CSR
        if (!message.hasConnectionSettingsRequest() ||
                !message.getConnectionSettingsRequest().hasOpamp() ||
                !message.getConnectionSettingsRequest().getOpamp().hasCertificateRequest()) {
            return errorResponse(message, "Missing CSR in enrollment request");
        }

        final ByteString csrBytes = message.getConnectionSettingsRequest()
                .getOpamp().getCertificateRequest().getCsr();
        if (csrBytes.isEmpty()) {
            return errorResponse(message, "Empty CSR");
        }

        try {
            // 3. Sign CSR with Enrollment CA
            final CertificateEntry enrollmentCa = enrollmentTokenService.getEnrollmentCa();
            final X509Certificate agentCert = certificateService.builder().signCsr(
                    csrBytes.toByteArray(), enrollmentCa, instanceUid, Duration.ofDays(365));

            // 4. Save agent record
            final String fingerprint = PemUtils.computeFingerprint(agentCert);
            final String certPem = PemUtils.toPem(agentCert);

            final OpAmpAgent agent = new OpAmpAgent(
                    null, instanceUid, auth.fleetId(), fingerprint, certPem,
                    enrollmentCa.id(), Instant.now());
            agentService.save(agent);

            LOG.info("Enrolled agent {} in fleet {}", instanceUid, auth.fleetId());

            // 5. Return certificate
            return ServerToAgent.newBuilder()
                    .setInstanceUid(message.getInstanceUid())
                    .setConnectionSettings(ConnectionSettingsOffers.newBuilder()
                            .setOpamp(OpAMPConnectionSettings.newBuilder()
                                    .setCertificate(TLSCertificate.newBuilder()
                                            .setCert(ByteString.copyFromUtf8(certPem))
                                            .setCaCert(ByteString.copyFromUtf8(enrollmentCa.certificate())))))
                    .build();
        } catch (Exception e) {
            LOG.error("Enrollment failed for agent {}", instanceUid, e);
            return errorResponse(message, "Enrollment failed: " + e.getMessage());
        }
    }

    private ServerToAgent handleIdentifiedMessage(AgentToServer message, OpAmpAuthContext.Identified auth) {
        LOG.info("Received OpAMP message via {} from identified agent {}",
                auth.transport(), auth.agent().instanceUid());

        // Acknowledge the message
        return ServerToAgent.newBuilder()
                .setInstanceUid(message.getInstanceUid())
                .build();
    }

    private ServerToAgent errorResponse(AgentToServer message, String errorMessage) {
        return ServerToAgent.newBuilder()
                .setInstanceUid(message.getInstanceUid())
                .setErrorResponse(ServerErrorResponse.newBuilder().setErrorMessage(errorMessage))
                .build();
    }

    private UUID bytesToUuid(byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
