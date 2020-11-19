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
package org.graylog.security.authservice.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog2.security.encryption.EncryptedValue;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = AuthServiceBackendTestRequest.Builder.class)
public abstract class AuthServiceBackendTestRequest {
    private static final String FIELD_BACKEND_ID = "backend_id";
    private static final String FIELD_BACKEND_CONFIGURATION = "backend_configuration";
    private static final String FIELD_USER_LOGIN = "user_login";

    @JsonProperty(FIELD_BACKEND_ID)
    public abstract Optional<String> backendId();

    @JsonProperty(FIELD_BACKEND_CONFIGURATION)
    public abstract AuthServiceBackendDTO backendConfiguration();

    @JsonProperty(FIELD_USER_LOGIN)
    public abstract Optional<UserLogin> userLogin();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AuthServiceBackendTestRequest.Builder();
        }

        @JsonProperty(FIELD_BACKEND_ID)
        public abstract Builder backendId(@Nullable String backendId);

        @JsonProperty(FIELD_BACKEND_CONFIGURATION)
        public abstract Builder backendConfiguration(AuthServiceBackendDTO backendConfiguration);

        @JsonProperty(FIELD_USER_LOGIN)
        public abstract Builder userLogin(@Nullable UserLogin userLogin);

        public abstract AuthServiceBackendTestRequest build();
    }

    @AutoValue
    public static abstract class UserLogin {
        public static final String FIELD_USERNAME = "username";
        public static final String FIELD_PASSWORD = "password";

        @JsonProperty(FIELD_USERNAME)
        public abstract String username();

        @JsonProperty(FIELD_PASSWORD)
        public abstract EncryptedValue password();

        @JsonCreator
        public static UserLogin create(@JsonProperty(FIELD_USERNAME) String username,
                                       @JsonProperty(FIELD_PASSWORD) EncryptedValue password) {
            return new AutoValue_AuthServiceBackendTestRequest_UserLogin(username, password);
        }
    }
}
