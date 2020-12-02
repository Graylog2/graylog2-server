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

import org.graylog2.plugin.security.PasswordAlgorithm;
import org.mindrot.jbcrypt.BCrypt;

import javax.inject.Inject;
import javax.inject.Named;

import static com.google.common.base.Preconditions.checkArgument;

public class BCryptPasswordAlgorithm implements PasswordAlgorithm {
    private static final String PREFIX = "{bcrypt}";
    private static final String SALT_PREFIX = "{salt}";

    private final Integer saltSize;

    @Inject
    public BCryptPasswordAlgorithm(@Named("user_password_bcrypt_salt_size") Integer saltSize) {
        this.saltSize = saltSize;
    }

    @Override
    public boolean supports(String hashedPassword) {
        return hashedPassword.startsWith(PREFIX) && hashedPassword.contains(SALT_PREFIX);
    }

    private String hash(String password, String salt) {
        return PREFIX + BCrypt.hashpw(password, salt) + SALT_PREFIX + salt;
    }

    @Override
    public String hash(String password) {
        return hash(password, BCrypt.gensalt(this.saltSize));
    }

    @Override
    public boolean matches(String hashedPasswordAndSalt, String otherPassword) {
        checkArgument(supports(hashedPasswordAndSalt), "Supplied hashed password is not supported, it does not start with "
                + PREFIX + " or does not contain a salt.");

        final int saltIndex = hashedPasswordAndSalt.lastIndexOf(SALT_PREFIX);
        final String salt = hashedPasswordAndSalt.substring(saltIndex + SALT_PREFIX.length());
        return hash(otherPassword, salt).equals(hashedPasswordAndSalt);
    }
}
