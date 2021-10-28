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

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@AutoValue
public abstract class UserDetails {
    public abstract Optional<String> databaseId();

    public abstract String authServiceType();

    public abstract String authServiceId();

    public abstract String base64AuthServiceUid();

    public abstract String username();

    public abstract boolean accountIsEnabled();

    public abstract String email();

    public abstract Optional<String> firstName();

    public abstract Optional<String> lastName();

    /**
     * Some authentication backends only currently support the fullName attribute (and not firstName and lastName),
     * so it is still optionally available here. Prefer use of only firstName and lastName when available.
     */
    public abstract Optional<String> fullName();

    public abstract boolean isExternal();

    public abstract Set<String> defaultRoles();

    public UserDetails withDatabaseId(String id) {
        checkArgument(!isNullOrEmpty(id), "id cannot be null or empty");

        return toBuilder().databaseId(id).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public static Builder create() {
            return new AutoValue_UserDetails.Builder();
        }

        public abstract Builder databaseId(@Nullable String databaseId);

        public abstract Builder authServiceType(String authServiceType);

        public abstract Builder authServiceId(String authServiceId);

        public abstract Builder base64AuthServiceUid(String base64AuthServiceUid);

        public abstract Builder username(String username);

        public abstract Builder accountIsEnabled(boolean isEnabled);

        public abstract Builder email(String email);

        public abstract Builder firstName(@Nullable String firstName);

        public abstract Builder lastName(@Nullable String lastName);

        /**
         * Starting in Graylog 4.1, use of this method is deprecated.
         * Prefer use of the {@link #firstName()} and {@link #lastName()} methods instead when possible. This way,
         * both individual first and last names will be available when needed.
         */
        @Deprecated
        public abstract Builder fullName(@Nullable String fullName);

        public abstract Builder isExternal(boolean isExternal);

        public abstract Builder defaultRoles(Set<String> defaultRoles);

        abstract UserDetails autoBuild();

        public UserDetails build() {
            UserDetails userDetails = autoBuild();

            // Either a fullName, or a firstName/lastName are required.
            final boolean missingFirstOrLast = !userDetails.firstName().isPresent()
                                               || !userDetails.lastName().isPresent();

            if (missingFirstOrLast && !userDetails.fullName().isPresent()) {
                throw new IllegalArgumentException("Either a firstName/lastName or a fullName are required.");
            }
            return userDetails;
        }
    }
}
