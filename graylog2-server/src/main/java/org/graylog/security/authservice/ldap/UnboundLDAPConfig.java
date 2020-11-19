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
public abstract class UnboundLDAPConfig {
    public abstract String userSearchBase();

    public abstract String userSearchPattern();

    public abstract String userUniqueIdAttribute();

    public abstract String userNameAttribute();

    public abstract String userFullNameAttribute();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public static Builder create() {
            return new AutoValue_UnboundLDAPConfig.Builder();
        }

        public abstract Builder userSearchBase(String userSearchBase);

        public abstract Builder userSearchPattern(String userSearchPattern);

        public abstract Builder userUniqueIdAttribute(String userUniqueIdAttribute);

        public abstract Builder userNameAttribute(String userNameAttribute);

        public abstract Builder userFullNameAttribute(String userFullNameAttribute);

        public abstract UnboundLDAPConfig build();
    }
}
