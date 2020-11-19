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

import org.apache.shiro.crypto.hash.SimpleHash;
import org.graylog2.plugin.security.PasswordAlgorithm;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.regex.Pattern;

public class SHA1HashPasswordAlgorithm implements PasswordAlgorithm {
    private static final String HASH_ALGORITHM = "SHA-1";
    private static final Pattern prefixPattern = Pattern.compile("^[a-f0-9]{40}$");
    private final String passwordSecret;

    @Inject
    public SHA1HashPasswordAlgorithm(@Named("password_secret") String passwordSecret) {
        this.passwordSecret = passwordSecret;
    }

    @Override
    public boolean supports(String hashedPassword) {
        return prefixPattern.matcher(hashedPassword).matches();
    }

    private String hash(String password, String salt) {
        return new SimpleHash(HASH_ALGORITHM, password, salt).toString();
    }

    @Override
    public String hash(String password) {
        return hash(password, passwordSecret);
    }

    @Override
    public boolean matches(String hashedPassword, String otherPassword) {
        if (supports(hashedPassword)) {
            return hash(otherPassword).equals(hashedPassword);
        } else {
            throw new IllegalArgumentException("Supplied hashed password is not supported.");
        }
    }
}
