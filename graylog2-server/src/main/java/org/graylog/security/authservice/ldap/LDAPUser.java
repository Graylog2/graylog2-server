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
package org.graylog.security.authservice.ldap;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class LDAPUser {
    public abstract String base64UniqueId();

    public abstract boolean accountIsEnabled();

    public abstract String username();

    public abstract String fullName();

    public abstract String email();

    public abstract LDAPEntry entry();

    public String dn() {
        return entry().dn();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public static Builder create() {
            return new AutoValue_LDAPUser.Builder();
        }

        public abstract Builder base64UniqueId(String base64UniqueId);

        public abstract Builder accountIsEnabled(boolean isEnabled);

        public abstract Builder username(String username);

        public abstract Builder fullName(String fullName);

        public abstract Builder email(String email);

        public abstract Builder entry(LDAPEntry entry);

        public abstract LDAPUser build();
    }
}
