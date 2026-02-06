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
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.security.certificates.Algorithm;
import org.graylog.security.certificates.CertificateEntry;
import org.graylog.security.certificates.CertificateService;
import org.graylog.security.certificates.PemUtils;
import org.graylog2.opamp.OpAmpAgent;
import org.graylog2.opamp.OpAmpAgentService;
import org.graylog2.opamp.config.OpAmpCaConfig;
import org.graylog2.opamp.rest.CreateEnrollmentTokenRequest;
import org.graylog2.opamp.rest.EnrollmentTokenResponse;
import org.graylog2.opamp.transport.OpAmpAuthContext;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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

    private static final Duration ROOT_CA_VALIDITY = Duration.ofDays(30 * 365);
    private static final Duration ENROLLMENT_CA_VALIDITY = Duration.ofDays(5 * 365);
    private static final Duration TOKEN_SIGNING_VALIDITY = Duration.ofDays(2 * 365);

    private static final String ROOT_CA_CN = "Graylog OpAMP Root CA";
    private static final String ENROLLMENT_CA_CN = "Graylog OpAMP Enrollment CA";
    private static final String TOKEN_SIGNING_CN = "Graylog OpAMP Token Signing";

    private static final String AUDIENCE_SUFFIX = ":opamp";

    private final CertificateService certificateService;
    private final ClusterConfigService clusterConfigService;
    private final OpAmpAgentService agentService;

    @Inject
    public EnrollmentTokenService(CertificateService certificateService,
                                  ClusterConfigService clusterConfigService,
                                  OpAmpAgentService agentService) {
        this.certificateService = certificateService;
        this.clusterConfigService = clusterConfigService;
        this.agentService = agentService;
    }

    private String getClusterId() {
        return clusterConfigService
                .getOrDefault(ClusterId.class, ClusterId.create("unknown"))
                .clusterId();
    }

    /**
     * Returns the token signing certificate for signing enrollment JWTs.
     * <p>
     * Creates the CA hierarchy on first call if it doesn't exist.
     *
     * @return the token signing certificate entry
     */
    public CertificateEntry getTokenSigningCert() {
        return ensureCaHierarchyExists().tokenSigningCert();
    }

    /**
     * Returns the enrollment CA certificate for signing agent CSRs.
     * <p>
     * Creates the CA hierarchy on first call if it doesn't exist.
     *
     * @return the enrollment CA certificate entry
     */
    public CertificateEntry getEnrollmentCa() {
        return ensureCaHierarchyExists().enrollmentCa();
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
        final CaHierarchy hierarchy = ensureCaHierarchyExists();
        final CertificateEntry signingCert = hierarchy.tokenSigningCert();

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
            final AtomicReference<OpAmpAgent> agentRef = new AtomicReference<>();

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

                        final OpAmpAgent agent = agentService.findByFingerprint(fingerprint)
                                .orElseThrow(() -> new SecurityException("Unknown agent fingerprint"));
                        agentRef.set(agent);
                        try {
                            final X509Certificate cert = PemUtils.parseCertificate(agent.certificatePem());
                            cert.checkValidity();
                            return cert.getPublicKey();
                        } catch (Exception e) {
                            throw new SecurityException("Failed to parse agent certificate", e);
                        }
                    })
                    .build()
                    .parseSignedClaims(token);

            return Optional.of(new OpAmpAuthContext.Identified(agentRef.get(), transport));
        } catch (Exception e) {
            LOG.warn("Agent token validation failed: {}", e.getMessage());
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

    /**
     * Ensures the CA hierarchy exists, creating it if necessary.
     * <p>
     * On first call (no {@link OpAmpCaConfig} exists), creates:
     * <ul>
     *   <li>Root CA (Ed25519, 30 years)</li>
     *   <li>Enrollment CA (intermediate, 5 years)</li>
     *   <li>Token Signing Cert (end-entity, 2 years)</li>
     * </ul>
     * <p>
     * If multiple nodes race to create the hierarchy, each creates its own certs
     * and writes to ClusterConfig. The last write wins, and some certs become orphans.
     * This is acceptable because tokens are validated by looking up the signing cert
     * by fingerprint (from the JWT's {@code kid} header), not by config reference.
     *
     * @return the certificate entries
     */
    private CaHierarchy ensureCaHierarchyExists() {
        OpAmpCaConfig config = clusterConfigService.get(OpAmpCaConfig.class);
        if (config != null) {
            return loadFromConfig(config);
        }

        LOG.info("Creating OpAMP CA hierarchy on first enrollment token request");

        try {
            final var builder = certificateService.builder();

            final CertificateEntry rootCa = certificateService.save(
                    builder.createRootCa(ROOT_CA_CN, Algorithm.ED25519, ROOT_CA_VALIDITY)
            );

            final CertificateEntry enrollmentCa = certificateService.save(
                    builder.createIntermediateCa(ENROLLMENT_CA_CN, rootCa, ENROLLMENT_CA_VALIDITY)
            );

            final CertificateEntry tokenSigningCert = certificateService.save(
                    builder.createEndEntityCert(TOKEN_SIGNING_CN, enrollmentCa, KeyUsage.digitalSignature, TOKEN_SIGNING_VALIDITY)
            );

            clusterConfigService.write(new OpAmpCaConfig(enrollmentCa.id(), tokenSigningCert.id()));

            // Re-read config in case another node won the race (ClusterConfig uses upsert)
            config = clusterConfigService.get(OpAmpCaConfig.class);
            return loadFromConfig(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OpAMP CA hierarchy", e);
        }
    }

    private CaHierarchy loadFromConfig(OpAmpCaConfig config) {
        final CertificateEntry enrollmentCa = certificateService.findById(config.enrollmentCaId())
                .orElseThrow(() -> new IllegalStateException(
                        "OpAMP CA config exists but enrollment CA not found: " + config.enrollmentCaId()));
        final CertificateEntry tokenSigningCert = certificateService.findById(config.tokenSigningCertId())
                .orElseThrow(() -> new IllegalStateException(
                        "OpAMP CA config exists but token signing cert not found: " + config.tokenSigningCertId()));
        return new CaHierarchy(enrollmentCa, tokenSigningCert);
    }

    private record CaHierarchy(CertificateEntry enrollmentCa, CertificateEntry tokenSigningCert) {}
}
