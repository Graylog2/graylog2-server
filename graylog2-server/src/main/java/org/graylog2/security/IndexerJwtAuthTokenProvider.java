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
package org.graylog2.security;

import com.google.common.base.Suppliers;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.spec.SecretKeySpec;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import com.github.joschi.jadconfig.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Caution, this provider returns not just the token itself but also the "Bearer " prefix, so the value can be directly
 * used as an HTTP header content.
 */
@Singleton
public class IndexerJwtAuthTokenProvider implements Provider<String> {
    private final Supplier<String> authHeaderBearerString;

    private static final Logger LOG = LoggerFactory.getLogger(IndexerJwtAuthTokenProvider.class);

    @Inject
    public IndexerJwtAuthTokenProvider(@Named("password_secret") String signingKey,
                                       @Named("indexer_jwt_auth_token_expiration_duration") final Duration tokenExpirationDuration,
                                       @Named("indexer_jwt_auth_token_caching_duration") final Duration cachingDuration) {
        authHeaderBearerString = Suppliers.memoizeWithExpiration(() -> {
            LOG.debug("Creating new JWT token, expiration set to {}", tokenExpirationDuration);
            return "Bearer " + createToken(signingKey.getBytes(StandardCharsets.UTF_8), tokenExpirationDuration);
        }, cachingDuration.toSeconds(), TimeUnit.SECONDS);
    }

    public static String createToken(final byte[] apiKeySecretBytes, final Duration tokenExpirationDuration) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        JwtBuilder builder = Jwts.builder().setId("graylog datanode connect " + nowMillis)
                .addClaims(Map.of("os_roles", "admin"))
                .setIssuedAt(now)
                .setSubject("admin")
                .setIssuer("graylog")
                .setNotBefore(now)
                .setExpiration(new Date(nowMillis + tokenExpirationDuration.toMilliseconds()))
                .signWith(signingKey, signatureAlgorithm);

        final var token = builder.compact();
        return token;
    }

    @Override
    public String get() {
        return authHeaderBearerString.get();
    }
}
