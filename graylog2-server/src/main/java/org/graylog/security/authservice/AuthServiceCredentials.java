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
package org.graylog.security.authservice;

import com.google.auto.value.AutoValue;
import org.graylog2.security.encryption.EncryptedValue;

@AutoValue
public abstract class AuthServiceCredentials {
    public abstract String username();

    public abstract EncryptedValue password();

    /**
     * Returns true if the subject is already authenticated and the authentication service backend doesn't need
     * to authenticate anymore.
     *
     * @return true if already authenticated, false otherwise
     */
    public abstract boolean isAuthenticated();

    public static AuthServiceCredentials create(String username, EncryptedValue password) {
        return builder()
                .username(username)
                .password(password)
                .isAuthenticated(false)
                .build();
    }

    public static AuthServiceCredentials createAuthenticated(String username) {
        return builder()
                .username(username)
                .password(EncryptedValue.createUnset())
                .isAuthenticated(true)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public static Builder create() {
            return new AutoValue_AuthServiceCredentials.Builder()
                    .isAuthenticated(false);
        }

        public abstract Builder username(String username);

        public abstract Builder password(EncryptedValue password);

        public abstract Builder isAuthenticated(boolean isAuthenticated);

        public abstract AuthServiceCredentials build();
    }
}
