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
package org.graylog2.security.jwt;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.base.Suppliers;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.configuration.RunsWithDataNode;
import org.graylog2.security.JwtSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class IndexerJwtAuthTokenProvider implements Provider<IndexerJwtAuthToken> {
    private final Supplier<Optional<String>> cachingSupplier;

    private static final Logger LOG = LoggerFactory.getLogger(IndexerJwtAuthTokenProvider.class);
    private final JwtSecret jwtSecret;
    private final Duration tokenExpirationDuration;
    private final Duration cachingDuration;

    @Inject
    public IndexerJwtAuthTokenProvider(JwtSecret jwtSecret,
                                       ElasticsearchClientConfiguration configuration,
                                       @RunsWithDataNode final boolean runsWithDataNode
    ) {
        this(jwtSecret, configuration.indexerJwtAuthTokenExpirationDuration(), configuration.indexerJwtAuthTokenCachingDuration(), runsWithDataNode || configuration.indexerUseJwtAuthentication());
    }

    public IndexerJwtAuthTokenProvider(JwtSecret jwtSecret, Duration tokenExpirationDuration, Duration cachingDuration, boolean useJwtAuthentication) {
        this.jwtSecret = jwtSecret;
        this.tokenExpirationDuration = tokenExpirationDuration;
        this.cachingDuration = cachingDuration;

        cachingSupplier = Suppliers.memoizeWithExpiration(() -> {
            // TODO: can we run this check outside of the supplier? Is the @RunsWithDataNode stable through the server lifetime?
            if (useJwtAuthentication) {
                LOG.info("Creating new JWT token, expiration set to {}", tokenExpirationDuration);
                return Optional.of(createToken(jwtSecret, tokenExpirationDuration));
            } else {
                return Optional.empty();
            }
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

    public IndexerJwtAuthToken get() {
        return new IndexerJwtAuthToken(cachingSupplier);
    }

    public Provider<IndexerJwtAuthToken> alwaysEnabled() {
        return new IndexerJwtAuthTokenProvider(jwtSecret, tokenExpirationDuration, cachingDuration, true);
    }
}
