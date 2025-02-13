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
import org.apache.commons.lang3.StringUtils;

@Singleton
public class JwtSecretProvider implements Provider<JwtSecret> {

    private final JwtSecret jwtSecret;

    @Inject
    public JwtSecretProvider(@Named("password_secret") final String passwordSecret) {
        this.jwtSecret = new JwtSecret(adaptPassword(passwordSecret));
    }

    /**
     * Our default requirement to secret password is length > 16 characters. But for JWT signing, we need at least 64.
     * So internally we have to extend the provided password to at least 64 if the actual length is not sufficient.
     *
     * @param passwordSecret configured password_secret, both from datanode and graylog server (needs to be the same)
     * @return signing secret extended to at least 64 characters
     */
    private String adaptPassword(String passwordSecret) {
        if (passwordSecret.length() < 64) {
            final String extended = passwordSecret + StringUtils.repeat(passwordSecret, 3);
            return extended.substring(0, 64);
        } else {
            return passwordSecret;
        }
    }

    @Override
    public JwtSecret get() {
        return this.jwtSecret;
    }
}
