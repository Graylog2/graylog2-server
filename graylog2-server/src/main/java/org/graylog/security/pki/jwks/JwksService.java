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
package org.graylog.security.pki.jwks;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.EdECPublicKey;
import java.time.Instant;
import java.util.Optional;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Service for exposing certificates as JSON Web Keys (JWKs).
 * <p>
 * This service provides methods for:
 * <ul>
 *   <li>Getting all valid signing keys as a JWKS (for the /.well-known/jwks endpoint)</li>
 *   <li>Looking up a single key by kid/fingerprint (for token validation)</li>
 *   <li>Getting certificate expiry (for capping token expiry)</li>
 * </ul>
 * <p>
 * Only certificates that are:
 * <ul>
 *   <li>Currently valid (notBefore &lt;= now &lt; notAfter)</li>
 *   <li>Have digitalSignature key usage</li>
 * </ul>
 * are exposed via this service.
 * <p>
 * TODO: Consider caching the JwksResponse if the endpoint is called frequently.
 *       The cache should be invalidated when certificates are added/removed/expire.
 */
@Singleton
public class JwksService {

    private static final Logger LOG = LoggerFactory.getLogger(JwksService.class);

    private final CertificateService certificateService;

    @Inject
    public JwksService(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /**
     * Returns all currently valid signing keys as a JWKS response.
     * <p>
     * Used by {@link JwksResource} for the /.well-known/jwks endpoint.
     *
     * @return the JWKS response containing all valid signing keys
     */
    public JwksResponse getJwks() {
        return certificateService.findAll().stream()
                .filter(this::isCurrentlyValid)
                .map(this::tryConvertToJwk)
                .flatMap(Optional::stream)
                .collect(collectingAndThen(toList(), JwksResponse::new));
    }

    /**
     * Looks up a single key by its kid (fingerprint).
     * <p>
     * Used for token validation. Returns empty if the key is not found,
     * expired, not yet valid, or doesn't have digitalSignature usage.
     *
     * @param kid the key ID (certificate fingerprint)
     * @return the JWK if found and valid, empty otherwise
     */
    public Optional<Jwk> findByKid(String kid) {
        return certificateService.findByFingerprint(kid)
                .filter(this::isCurrentlyValid)
                .flatMap(this::tryConvertToJwk);
    }

    /**
     * Returns the expiry time of a certificate by its kid (fingerprint).
     * <p>
     * Used to cap enrollment token expiry to certificate expiry.
     *
     * @param kid the key ID (certificate fingerprint)
     * @return the certificate's notAfter time, or empty if not found
     */
    public Optional<Instant> getCertExpiry(String kid) {
        return certificateService.findByFingerprint(kid)
                .map(CertificateEntry::notAfter);
    }

    /**
     * Checks if a certificate is currently valid.
     *
     * @param entry the certificate entry
     * @return true if notBefore &lt;= now &lt; notAfter
     */
    private boolean isCurrentlyValid(CertificateEntry entry) {
        final Instant now = Instant.now();
        return !now.isBefore(entry.notBefore()) && now.isBefore(entry.notAfter());
    }

    /**
     * Attempts to convert a certificate entry to a JWK.
     * <p>
     * Parses the certificate once and checks both digitalSignature key usage
     * and algorithm support before converting to JWK.
     *
     * @param entry the certificate entry
     * @return the JWK, or empty if the certificate doesn't have digitalSignature usage
     *         or uses an unsupported algorithm
     */
    private Optional<Jwk> tryConvertToJwk(CertificateEntry entry) {
        try {
            final X509Certificate cert = PemUtils.parseCertificate(entry.certificate());

            // Check digitalSignature key usage (keyUsage[0])
            final boolean[] keyUsage = cert.getKeyUsage();
            if (keyUsage == null || !keyUsage[0]) {
                return Optional.empty();
            }

            final PublicKey publicKey = cert.getPublicKey();
            final String kid = entry.fingerprint();

            return switch (publicKey) {
                case EdECPublicKey k -> Optional.of(OkpJwk.fromPublicKey(kid, k));
                default -> {
                    LOG.error("Unsupported key algorithm for JWKS: {}. Only Ed25519 is supported.",
                            publicKey.getAlgorithm());
                    yield Optional.empty();
                }
            };
        } catch (Exception e) {
            LOG.error("Failed to convert certificate to JWK: {}", entry.fingerprint(), e);
            return Optional.empty();
        }
    }
}
