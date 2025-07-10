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
package org.graylog2.security.hashing;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkArgument;

public class PBKDF2PasswordAlgorithm implements PasswordAlgorithm {
    private final Logger LOG = LoggerFactory.getLogger(PBKDF2PasswordAlgorithm.class);

    private final static String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512";
    private final static String PREFIX = "{PBKDF2}";

    public static final int DEFAULT_ITERATIONS = 10000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int BASE64_ENC_ESTIMATE_32BYTES = 44;

    private final int creationIterations;
    private final SecretKeyFactory secretKeyFactory;

    @Inject
    public PBKDF2PasswordAlgorithm(@Named("user_password_pbkdf2_iterations") int iterations) {
        this.creationIterations = iterations;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Unable to get PBKDF2withHMACSHA512 algorithm", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supports(String hashedPassword) {
        return hashedPassword.startsWith(PREFIX) && hashedPassword.chars().filter(ch -> ch == '%').count() == 2;
    }

    @Override
    public String hash(String password) {
        try {
            final byte[] saltBytes = new byte[32];
            new SecureRandom().nextBytes(saltBytes);
            // capacity is the various prefixes, iteration string, and the estimated base64 length of the two 32 byte salt and key lengths (~44 bytes each )
            final var result = new StringBuilder(PREFIX.length() + String.valueOf(creationIterations).length() + 2 /* % chars */ + 2 * BASE64_ENC_ESTIMATE_32BYTES)
                    .append(PREFIX)
                    .append(creationIterations)
                    .append("%")
                    .append(Base64.getEncoder().encodeToString(saltBytes))
                    .append("%");
            final PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), saltBytes, creationIterations, KEY_LENGTH_BITS);
            final var hash = secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();
            result.append(Base64.getEncoder().encodeToString(hash));
            return result.toString();
        } catch (InvalidKeySpecException e) {
            LOG.error("Unable to create PBKDF2 hash", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean matches(String hashedPasswordAndSalt, String otherPassword) {
        checkArgument(supports(hashedPasswordAndSalt), "Supplied hashed password is not supported, it does not start with "
                + PREFIX + " or does not contain a salt.");

        // supports ensures we have exactly 2 % chars, so splitting to 3 always works and contains strings for each array index
        final String[] split = hashedPasswordAndSalt.split("%", 3);
        final var iterationsInHash = Integer.parseInt(split[0].substring(PREFIX.length()));
        final byte[] saltBytes = Base64.getDecoder().decode(split[1].getBytes(StandardCharsets.UTF_8));
        final var hash = Base64.getDecoder().decode(split[2]);

        try {
            final PBEKeySpec pbeKeySpec = new PBEKeySpec(otherPassword.toCharArray(), saltBytes, iterationsInHash, KEY_LENGTH_BITS);
            final var otherHash = secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();

            return constantTimeEquals(hash, otherHash);
        } catch (InvalidKeySpecException e) {
            LOG.error("Unable to create PBKDF2 hash", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Compare strings for equality, but don't leak the position of the first inequality by checking all possible characters.
     * Defends against timing attacks.
     *
     * @param a input bytes[]
     * @param b input bytes[]
     * @return whether a == b for each character
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int equals = 0;
        for (int i = 0; i < a.length; i++) {
            equals |= a[i] ^ b[i];
        }

        return equals == 0;
    }
}
