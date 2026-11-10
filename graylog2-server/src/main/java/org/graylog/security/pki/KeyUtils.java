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
package org.graylog.security.pki;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * Utilities for cryptographic key pairs.
 */
public final class KeyUtils {
    private static final String SHA256_PREFIX = "SHA256:";
    private static final String BOUNCY_CASTLE_PROVIDER = "BC";

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private KeyUtils() {
    }

    /**
     * Generates a cryptographic key pair using the specified algorithm.
     * Uses the BouncyCastle provider for key generation.
     *
     * @param algorithm the algorithm to use for key generation
     * @return a newly generated key pair
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchProviderException  if the BouncyCastle provider is not available
     */
    public static KeyPair generateKeyPair(Algorithm algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm.keyAlgorithm(), BOUNCY_CASTLE_PROVIDER);
        if (algorithm == Algorithm.RSA_4096) {
            keyGen.initialize(algorithm.keySize(), new SecureRandom());
        }
        return keyGen.generateKeyPair();
    }

    /**
     * Computes the SHA-256 fingerprint of the public key from the given key pair.
     *
     * @param keyPair the key pair whose public key fingerprint to compute
     * @return the fingerprint in {@code SHA256:<base64>} format (matching ssh-keygen output)
     * @throws NoSuchAlgorithmException if the SHA-256 digest algorithm is not available
     */
    public static String sha256Fingerprint(KeyPair keyPair) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] hash = digest.digest(keyPair.getPublic().getEncoded());
        final String encoded = Base64.getEncoder().withoutPadding().encodeToString(hash);
        return SHA256_PREFIX + encoded;
    }
}
