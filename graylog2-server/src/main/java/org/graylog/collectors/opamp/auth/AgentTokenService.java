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
package org.graylog.collectors.opamp.auth;

import com.google.common.base.Throwables;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.security.pki.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.time.Clock;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class AgentTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(AgentTokenService.class);

    private final CollectorInstanceService collectorInstanceService;
    private final Clock clock;

    @Inject
    public AgentTokenService(CollectorInstanceService collectorInstanceService, Clock clock) {
        this.collectorInstanceService = collectorInstanceService;
        this.clock = clock;
    }

    /**
     * Validates an agent token and extracts the auth context.
     * <p>
     * Agent tokens are JWTs signed by the agent's private key. The JWT header contains
     * an {@code x5t#S256} claim with the certificate thumbprint (RFC 7515 format), which
     * is used to look up the agent and retrieve its public key for signature verification.
     * <p>
     * Validation includes:
     * <ul>
     *   <li>Extracting {@code x5t#S256} thumbprint from JWT header</li>
     *   <li>Converting from base64url to our fingerprint format for lookup</li>
     *   <li>Looking up agent by fingerprint</li>
     *   <li>Parsing agent's certificate and verifying validity</li>
     *   <li>Signature verification using the certificate's public key</li>
     *   <li>Expiration check (handled automatically by JJWT)</li>
     * </ul>
     *
     * @param token     the JWT token string
     * @param transport the transport type (HTTP or WEBSOCKET)
     * @return the identified context if valid, empty otherwise
     */
    public Optional<OpAmpAuthContext.Identified> validateAgentToken(String token, OpAmpAuthContext.Transport transport) {
        try {
            final AtomicReference<CollectorInstanceDTO> collectorRef = new AtomicReference<>();
            final var now = Date.from(clock.instant());

            Jwts.parser()
                    .keyLocator(header -> {
                        final String x5t = (String) header.get("x5t#S256");
                        if (x5t == null) {
                            throw new SecurityException("Missing x5t#S256 header");
                        }

                        // Convert from base64url to our fingerprint format for lookup
                        final String fingerprint;
                        try {
                            fingerprint = PemUtils.x5tToFingerprint(x5t);
                        } catch (Exception e) {
                            throw new SecurityException("Invalid x5t#S256 format: " + e.getMessage());
                        }

                        // TODO performance this loads the entire collector instance document, which seems excessive
                        final CollectorInstanceDTO collector = collectorInstanceService.findByActiveOrNextFingerprint(fingerprint)
                                .orElseThrow(() -> new SecurityException("Unknown collector fingerprint"));
                        collectorRef.set(collector);
                        return selectPublicKey(collector, fingerprint, now);
                    })
                    .clock(() -> now)
                    .build()
                    .parseSignedClaims(token);

            return Optional.of(new OpAmpAuthContext.Identified(collectorRef.get().instanceUid(), transport));
        } catch (Exception e) {
            LOG.warn("Agent token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private PublicKey selectPublicKey(CollectorInstanceDTO instance, String fingerprint, Date now) {
        final var nextCertFp = instance.nextCertificateFingerprint().orElse(null);

        try {
            // If the instance authenticates with its new certificate, activate the new one.
            // The renewal process was successful.
            if (fingerprint.equals(nextCertFp) && instance.nextCertificatePem().isPresent()) {
                final var publicKey = getPublicKey(instance.nextCertificatePem().get(), now);
                LOG.info("Activating next certificate for instance: {}", instance.instanceUid());
                if (!collectorInstanceService.activateNextCertificate(instance)) {
                    LOG.warn("Failed to activate next certificate for instance: {}", instance.instanceUid());
                }
                return publicKey;
            }
        } catch (SecurityException e) {
            LOG.warn("Token validation with next certificate failed. Falling back to active certificate: {}",
                    Throwables.getRootCause(e).getMessage());
            // TODO: Store error message in instance document
        }
        return getPublicKey(instance.activeCertificatePem(), now);
    }

    private PublicKey getPublicKey(String certPem, Date now) {
        try {
            final var cert = PemUtils.parseCertificate(certPem);
            cert.checkValidity(now);
            return cert.getPublicKey();
        } catch (IOException | CertificateException e) {
            throw new SecurityException("Failed to parse collector certificate", e);
        }
    }
}
