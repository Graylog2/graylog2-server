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
package org.graylog2.security.sessions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.apache.shiro.session.mgt.SimpleSession;
import org.graylog2.database.BuildableMongoEntity;

import java.time.Instant;
import java.util.Optional;

@AutoValue
@JsonSerialize(as = SessionDTO.class)
@JsonDeserialize(builder = SessionDTO.Builder.class)
public abstract class SessionDTO implements BuildableMongoEntity<SessionDTO, SessionDTO.Builder> {
    public static final String USERNAME_SESSION_KEY = "username";
    public static final String AUTH_CONTEXT_SESSION_KEY = "auth_context";

    public static final String FIELD_SESSION_ID = "session_id";
    public static final String FIELD_TIMEOUT = "timeout";
    public static final String FIELD_START_TIMESTAMP = "start_timestamp";
    public static final String FIELD_LAST_ACCESS_TIME = "last_access_time";
    public static final String FIELD_EXPIRED = "expired";
    public static final String FIELD_HOST = "host";
    public static final String FIELD_AUTHENTICATION_REALM = "authentication_realm";
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_USER_NAME = "user_name";
    public static final String FIELD_AUTHENTICATED = "authenticated";
    public static final String FIELD_AUTH_CONTEXT = "auth_context";

    @JsonProperty(FIELD_SESSION_ID)
    public abstract String sessionId();

    @JsonProperty(FIELD_TIMEOUT)
    public abstract long timeout();

    @JsonProperty(FIELD_START_TIMESTAMP)
    public abstract Instant startTimestamp();

    @JsonProperty(FIELD_LAST_ACCESS_TIME)
    public abstract Instant lastAccessTime();

    @JsonProperty(FIELD_EXPIRED)
    public abstract boolean expired();

    @JsonProperty(FIELD_HOST)
    public abstract String host();

    @JsonProperty(FIELD_AUTHENTICATION_REALM)
    public abstract Optional<String> authenticationRealm();

    @JsonProperty(FIELD_USER_ID)
    public abstract Optional<String> userId();

    @JsonProperty(FIELD_USER_NAME)
    public abstract Optional<String> userName();

    @JsonProperty(FIELD_AUTHENTICATED)
    abstract Optional<Boolean> authenticated();

    @JsonProperty(FIELD_AUTH_CONTEXT)
    public abstract Optional<SessionAuthContext> authContext();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<SessionDTO, Builder> {
        @JsonProperty(FIELD_SESSION_ID)
        public abstract Builder sessionId(String sessionId);

        @JsonProperty(FIELD_TIMEOUT)
        public abstract Builder timeout(long timeout);

        @JsonProperty(FIELD_START_TIMESTAMP)
        public abstract Builder startTimestamp(Instant startTimestamp);

        @JsonProperty(FIELD_LAST_ACCESS_TIME)
        public abstract Builder lastAccessTime(Instant lastAccessTime);

        @JsonProperty(FIELD_EXPIRED)
        public abstract Builder expired(boolean expired);

        @JsonProperty(FIELD_HOST)
        public abstract Builder host(String host);

        @JsonProperty(FIELD_AUTHENTICATION_REALM)
        public abstract Builder authenticationRealm(@Nullable String authenticationRealm);

        @JsonProperty(FIELD_USER_ID)
        public abstract Builder userId(@Nullable String userId);

        @JsonProperty(FIELD_USER_NAME)
        public abstract Builder userName(@Nullable String userName);

        @JsonProperty(FIELD_AUTHENTICATED)
        public abstract Builder authenticated(@Nullable Boolean authenticated);

        @JsonProperty(FIELD_AUTH_CONTEXT)
        public abstract Builder authContext(@Nullable SessionAuthContext authContext);

        public abstract SessionDTO build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SessionDTO.Builder();
        }
    }

    public static SessionDTO fromSimpleSession(SimpleSession simpleSession) {
        return SessionConverter.simpleSessionToSessionDTO(simpleSession);
    }

    public SimpleSession toSimpleSession() {
        return SessionConverter.sessionDTOToSimpleSession(this);
    }
}

