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

import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.security.pki.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class AgentTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(AgentTokenService.class);

    private final CollectorInstanceService collectorInstanceService;

    @Inject
    public AgentTokenService(CollectorInstanceService collectorInstanceService) {
        this.collectorInstanceService = collectorInstanceService;
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
                        final CollectorInstanceDTO collector = collectorInstanceService.findByFingerprint(fingerprint)
                                .orElseThrow(() -> new SecurityException("Unknown collector fingerprint"));
                        collectorRef.set(collector);
                        try {
                            final X509Certificate cert = PemUtils.parseCertificate(collector.certificatePem());
                            cert.checkValidity();
                            return cert.getPublicKey();
                        } catch (Exception e) {
                            throw new SecurityException("Failed to parse collector certificate", e);
                        }
                    })
                    .build()
                    .parseSignedClaims(token);

            return Optional.of(new OpAmpAuthContext.Identified(collectorRef.get().instanceUid(), transport));
        } catch (Exception e) {
            LOG.warn("Agent token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

}
