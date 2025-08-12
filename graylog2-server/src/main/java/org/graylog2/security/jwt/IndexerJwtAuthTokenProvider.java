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
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.configuration.RunsWithDataNode;
import org.graylog2.security.JwtSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
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

    /**
     * Clock skew tolerance should be datanode/opensearch setting only. But there is a bug in current opensearch and the tolerance
     * will be available only in 3.2 and newer (https://github.com/opensearch-project/security/pull/5506)
     * Till then, we can work around it by generating tokens with extended validity in both directions
     */
    @Deprecated(forRemoval = true)
    private final Duration clockSkewTolerance;
    private final Clock clock;

    @Inject
    public IndexerJwtAuthTokenProvider(JwtSecret jwtSecret,
                                       ElasticsearchClientConfiguration configuration,
                                       @RunsWithDataNode final boolean runsWithDataNode
    ) {
        this(jwtSecret,
                configuration.indexerJwtAuthTokenExpirationDuration(),
                configuration.indexerJwtAuthTokenCachingDuration(),
                configuration.getIndexerJwtAuthTokenClockSkewTolerance(),
                runsWithDataNode || configuration.indexerUseJwtAuthentication(),
                Clock.systemDefaultZone()
        );
    }

    public IndexerJwtAuthTokenProvider(
            JwtSecret jwtSecret, Duration tokenExpirationDuration, Duration cachingDuration, @Deprecated Duration clockSkewTolerance, boolean useJwtAuthentication, Clock clock) {
        this.jwtSecret = jwtSecret;
        this.tokenExpirationDuration = tokenExpirationDuration;
        this.cachingDuration = cachingDuration;
        this.clockSkewTolerance = clockSkewTolerance;
        this.clock = clock;
        cachingSupplier = Suppliers.memoizeWithExpiration(() -> {
            if (useJwtAuthentication) {
                LOG.debug("Creating new JWT token, expiration set to {}", tokenExpirationDuration);
                return Optional.of(createToken());
            } else {
                return Optional.empty();
            }
        }, cachingDuration.toSeconds(), TimeUnit.SECONDS);
    }

    private String createToken() {
        long nowMillis = clock.millis();
        Date now = new Date(nowMillis);

        JwtBuilder builder = Jwts.builder()
                .id("graylog datanode connect " + nowMillis)
                .claims(Map.of("os_roles", "admin"))
                .issuedAt(now)
                .subject("admin")
                .issuer("graylog")
                .notBefore(getNotBefore(nowMillis))
                .expiration(getExpiration(nowMillis))
                .signWith(jwtSecret.getSigningKey());

        return builder.compact();
    }

    @Nonnull
    private Date getNotBefore(long nowMillis) {
        // TODO: since we can't configure clock skew tolerance in opensearch, we have to work around it. See https://github.com/opensearch-project/security/pull/5506
        // After 3.2 release the NBF claim should be just now. Any acceptable time drift will be corrected by clock skew tolerance setting in opensearch
        return new Date(nowMillis - clockSkewTolerance.toMilliseconds());
    }

    @Nonnull
    private Date getExpiration(long nowMillis) {
        // TODO: since we can't configure clock skew tolerance in opensearch, we have to work around it. See https://github.com/opensearch-project/security/pull/5506
        // After 3.2 release the expiration claim should be just now + token expiration duration
        return new Date(nowMillis + tokenExpirationDuration.toMilliseconds() + clockSkewTolerance.toMilliseconds());
    }

    public IndexerJwtAuthToken get() {
        return new IndexerJwtAuthToken(cachingSupplier);
    }

    public Provider<IndexerJwtAuthToken> alwaysEnabled() {
        return new IndexerJwtAuthTokenProvider(jwtSecret, tokenExpirationDuration, cachingDuration, clockSkewTolerance, true, Clock.systemDefaultZone());
    }
}
