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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

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
    public IndexerJwtAuthTokenProvider(JwtSecret jwtSecret,
                                       @Named("indexer_jwt_auth_token_expiration_duration") final Duration tokenExpirationDuration,
                                       @Named("indexer_jwt_auth_token_caching_duration") final Duration cachingDuration) {
        authHeaderBearerString = Suppliers.memoizeWithExpiration(() -> {
            LOG.debug("Creating new JWT token, expiration set to {}", tokenExpirationDuration);
            return "Bearer " + createToken(jwtSecret, tokenExpirationDuration);
        }, cachingDuration.toSeconds(), TimeUnit.SECONDS);
    }

    public static String createToken(final JwtSecret jwtSecret, final Duration tokenExpirationDuration) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        JwtBuilder builder = Jwts.builder()
                .id("graylog datanode connect " + nowMillis)
                .claims(Map.of("os_roles", "admin"))
                .issuedAt(now)
                .subject("admin")
                .issuer("graylog")
                .notBefore(now)
                .expiration(new Date(nowMillis + tokenExpirationDuration.toMilliseconds()))
                .signWith(jwtSecret.getSigningKey());

        return builder.compact();
    }

    @Override
    public String get() {
        return authHeaderBearerString.get();
    }
}
