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
package org.graylog2.opamp.enrollment;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog2.opamp.OpAmpCaService;
import org.graylog2.opamp.rest.CreateEnrollmentTokenRequest;
import org.graylog2.opamp.rest.EnrollmentTokenResponse;
import org.graylog2.opamp.transport.OpAmpAuthContext;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Service for accessing enrollment certificates.
 * <p>
 * The CA hierarchy is created on-demand when the first enrollment token is requested.
 * This avoids creating certificates for deployments that don't use OpAMP enrollment.
 */
@Singleton
public class EnrollmentTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(EnrollmentTokenService.class);

    private static final String AUDIENCE_SUFFIX = ":opamp";

    private final CertificateService certificateService;
    private final ClusterConfigService clusterConfigService;
    private final CollectorInstanceService collectorInstanceService;
    private final OpAmpCaService opAmpCaService;

    @Inject
    public EnrollmentTokenService(CertificateService certificateService,
                                  ClusterConfigService clusterConfigService,
                                  CollectorInstanceService collectorInstanceService,
                                  OpAmpCaService opAmpCaService) {
        this.certificateService = certificateService;
        this.clusterConfigService = clusterConfigService;
        this.collectorInstanceService = collectorInstanceService;
        this.opAmpCaService = opAmpCaService;
    }

    private String getClusterId() {
        final var clusterId = clusterConfigService.get(ClusterId.class);
        if (clusterId == null || isNullOrEmpty(clusterId.clusterId())) {
            throw new IllegalStateException("Missing or empty Cluster ID. This should not happen.");
        }
        return clusterId.clusterId();
    }

    /**
     * Returns the token signing certificate for signing enrollment JWTs.
     * <p>
     * Delegates to {@link OpAmpCaService} which creates the CA hierarchy on first call if it doesn't exist.
     *
     * @return the token signing certificate entry
     */
    public CertificateEntry getTokenSigningCert() {
        return opAmpCaService.getTokenSigningCert();
    }

    /**
     * Returns the enrollment CA certificate for signing agent CSRs.
     * <p>
     * Delegates to {@link OpAmpCaService} which creates the CA hierarchy on first call if it doesn't exist.
     *
     * @return the enrollment CA certificate entry
     */
    public CertificateEntry getEnrollmentCa() {
        return opAmpCaService.getOpAmpCa();
    }

    /**
     * Creates a new enrollment token for OpAMP agents.
     * <p>
     * The token is a JWT signed with the token signing certificate's private key.
     * The JWT contains:
     * <ul>
     *   <li>{@code kid} header: fingerprint of the signing certificate</li>
     *   <li>{@code iss}: cluster ID (informational, not validated)</li>
     *   <li>{@code aud}: {@code {cluster_id}:opamp} (validated on receipt)</li>
     *   <li>{@code iat}: current timestamp</li>
     *   <li>{@code exp}: expiration timestamp</li>
     *   <li>{@code fleet_id}: optional fleet identifier</li>
     * </ul>
     *
     * @param request the token creation request
     * @return the created token and its expiration time
     * @throws IllegalArgumentException if the requested expiry exceeds the signing certificate's validity
     */
    public EnrollmentTokenResponse createToken(CreateEnrollmentTokenRequest request) {
        final CertificateEntry signingCert = opAmpCaService.getTokenSigningCert();

        final Duration expiresIn = request.expiresIn() != null
                ? request.expiresIn()
                : CreateEnrollmentTokenRequest.DEFAULT_EXPIRY;

        final Instant now = Instant.now();
        final Instant expiresAt = now.plus(expiresIn);

        // Validate against cert expiry
        final Instant certExpiry = signingCert.notAfter();
        if (expiresAt.isAfter(certExpiry)) {
            throw new IllegalArgumentException(
                    f("Token expiry %s exceeds signing certificate expiry %s", expiresAt, certExpiry));
        }

        final String token = buildJwt(signingCert, request.fleetId(), now, expiresAt);

        return new EnrollmentTokenResponse(token, expiresAt);
    }

    /**
     * Validates an enrollment token and extracts the auth context.
     * <p>
     * Validation includes:
     * <ul>
     *   <li>Signature verification using the cert identified by {@code kid} header</li>
     *   <li>Expiration check (handled automatically by JJWT)</li>
     *   <li>Audience validation (must be {@code {cluster_id}:opamp})</li>
     *   <li>Required {@code fleet_id} claim</li>
     * </ul>
     *
     * @param token the JWT token string
     * @param transport the transport type (HTTP or WEBSOCKET)
     * @return the enrollment context if valid, empty otherwise
     */
    public Optional<OpAmpAuthContext.Enrollment> validateToken(String token, OpAmpAuthContext.Transport transport) {
        try {
            final String expectedAudience = getClusterId() + AUDIENCE_SUFFIX;

            final Jws<Claims> jws = Jwts.parser()
                    .keyLocator(header -> {
                        final String kid = (String) header.get("kid");
                        if (kid == null) {
                            throw new SecurityException("Missing kid header");
                        }
                        return certificateService.findByFingerprint(kid)
                                .map(cert -> {
                                    try {
                                        return PemUtils.parseCertificate(cert.certificate()).getPublicKey();
                                    } catch (Exception e) {
                                        throw new SecurityException("Failed to parse certificate", e);
                                    }
                                })
                                .orElseThrow(() -> new SecurityException("Unknown kid: " + kid));
                    })
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(token);

            final String fleetId = jws.getPayload().get("fleet_id", String.class);
            if (fleetId == null || fleetId.isBlank()) {
                LOG.warn("Enrollment token validation failed: missing fleet_id");
                return Optional.empty();
            }

            return Optional.of(new OpAmpAuthContext.Enrollment(fleetId, transport));
        } catch (Exception e) {
            LOG.warn("Enrollment token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
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
     * @param token the JWT token string
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
            LOG.warn("Agent token validation failed.", e);
            return Optional.empty();
        }
    }

    private String buildJwt(CertificateEntry signingCert, String fleetId,
                            Instant issuedAt, Instant expiresAt) {
        try {
            final String privateKeyPem = certificateService.encryptedValueService()
                    .decrypt(signingCert.privateKey());
            final PrivateKey privateKey = PemUtils.parsePrivateKey(privateKeyPem);

            final String clusterId = getClusterId();
            final String audience = clusterId + AUDIENCE_SUFFIX;

            var builder = Jwts.builder()
                    .header()
                    .add("ctt", "enrollment")
                        .add("kid", signingCert.fingerprint())
                    .and()
                    .issuer(clusterId)
                    .audience().add(audience).and()
                    .issuedAt(Date.from(issuedAt))
                    .expiration(Date.from(expiresAt));

            if (fleetId != null) {
                builder.claim("fleet_id", fleetId);
            }

            return builder.signWith(privateKey).compact();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build enrollment JWT", e);
        }
    }

}
