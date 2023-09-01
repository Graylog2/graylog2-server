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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Singleton
public class JwtBearerTokenProvider implements Provider<String> {
    private final Supplier<String> authHeaderBearerString;
    private final Duration tokenExpirationDuration;

    @Inject
    public JwtBearerTokenProvider(@Named("password_secret") String signingKey,
                                  @Named("opensearch_jwt_token_expiration_duration") final Duration  tokenExpirationDuration,
                                  @Named("opensearch_jwt_token_caching_duration") final Duration cachingDuration) {
        this.tokenExpirationDuration = tokenExpirationDuration;
        authHeaderBearerString = Suppliers.memoizeWithExpiration(() -> "Bearer " + createToken(signingKey.getBytes(StandardCharsets.UTF_8)), cachingDuration.getSeconds(), TimeUnit.SECONDS);
    }

   private String createToken(byte[] apiKeySecretBytes) {
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
                .setExpiration(new Date(nowMillis + tokenExpirationDuration.toMillis()))
                .signWith(signingKey, signatureAlgorithm);

        return builder.compact();
    }

    @Override
    public String get() {
        return authHeaderBearerString.get();
    }
}
