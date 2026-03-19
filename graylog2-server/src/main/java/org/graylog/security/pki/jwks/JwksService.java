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
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.TokenSigningKey;
import org.graylog.security.pki.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.security.interfaces.EdECPublicKey;
import java.util.List;
import java.util.Optional;

/**
 * Service for exposing signing keys as JSON Web Keys (JWKs).
 * TODO: Consider caching the JwksResponse if the endpoint is called frequently.
 *       The cache should be invalidated when certificates are added/removed/expire.
 */
@Singleton
public class JwksService {
    private static final Logger LOG = LoggerFactory.getLogger(JwksService.class);

    private final CollectorsConfigService collectorsConfigService;

    @Inject
    public JwksService(CollectorsConfigService collectorsConfigService) {
        this.collectorsConfigService = collectorsConfigService;
    }

    /**
     * Returns all currently valid signing keys as a JWKS response.
     * <p>
     * Used by {@link JwksResource} for the /.well-known/jwks endpoint.
     *
     * @return the JWKS response containing all valid signing keys
     */
    public JwksResponse getJwks() {
        final var tokenSigningKey = collectorsConfigService.get().map(CollectorsConfig::tokenSigningKey);

        if (tokenSigningKey.isPresent()) {
            return new JwksResponse(tokenSigningKey.flatMap(this::tryConvertToJwk).map(List::of).orElse(List.of()));
        }
        return new JwksResponse(List.of());
    }

    /**
     * Attempts to convert a token signing key to a JWK.
     *
     * @param tokenSigningKey the signing key
     * @return the JWK, or empty if we can't parse the public signing key
     */
    private Optional<Jwk> tryConvertToJwk(TokenSigningKey tokenSigningKey) {
        try {
            final PublicKey publicKey = PemUtils.parsePublicKey(tokenSigningKey.publicKey());
            final String kid = tokenSigningKey.fingerprint();

            return switch (publicKey) {
                case EdECPublicKey k -> Optional.of(OkpJwk.fromPublicKey(kid, k));
                default -> {
                    LOG.error("Unsupported key algorithm for JWKS: {}. Only Ed25519 is supported.", publicKey.getAlgorithm());
                    yield Optional.empty();
                }
            };
        } catch (Exception e) {
            LOG.error("Failed to convert certificate to JWK: {}", tokenSigningKey.fingerprint(), e);
            return Optional.empty();
        }
    }
}
