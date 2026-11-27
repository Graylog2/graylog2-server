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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.nio.charset.StandardCharsets;

@Singleton
public class JwtSecretProvider implements Provider<JwtSecret> {

    /**
     * HKDF info parameter used to derive the JWT signing key from the password_secret. Graylog server and datanode
     * must derive the same key, so changing this value breaks the authentication between them.
     */
    private static final byte[] JWT_SIGNING_KEY_HKDF_INFO = "graylog-indexer-jwt-signing-key-v1".getBytes(StandardCharsets.UTF_8);

    /**
     * 64 bytes, so the key is accepted for HS512 signatures.
     */
    private static final int JWT_SIGNING_KEY_LENGTH_BYTES = 64;

    private final JwtSecret jwtSecret;

    @Inject
    public JwtSecretProvider(@Named("password_secret") final String passwordSecret) {
        this.jwtSecret = new JwtSecret(deriveSigningKey(passwordSecret));
    }

    /**
     * Derive a dedicated JWT signing key from the password_secret, so the secret itself is never used as a signing
     * key nor written to the opensearch security configuration.
     *
     * @param passwordSecret configured password_secret, both from datanode and graylog server (needs to be the same)
     * @return signing key derived from the password_secret
     */
    private static byte[] deriveSigningKey(String passwordSecret) {
        final HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(passwordSecret.getBytes(StandardCharsets.UTF_8), null, JWT_SIGNING_KEY_HKDF_INFO));
        final byte[] derived = new byte[JWT_SIGNING_KEY_LENGTH_BYTES];
        hkdf.generateBytes(derived, 0, derived.length);
        return derived;
    }

    @Override
    public JwtSecret get() {
        return this.jwtSecret;
    }
}
