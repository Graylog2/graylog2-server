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
import com.google.common.collect.Iterables;
import jakarta.annotation.Nullable;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.graylog2.database.BuildableMongoEntity;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AutoValue
@JsonSerialize(as = SessionDTO.class)
@JsonDeserialize(builder = SessionDTO.Builder.class)
public abstract class SessionDTO implements BuildableMongoEntity<SessionDTO, SessionDTO.Builder> {

    public static final String FIELD_SESSION_ID = "session_id";
    public static final String FIELD_TIMEOUT = "timeout";
    public static final String FIELD_START_TIMESTAMP = "start_timestamp";
    public static final String FIELD_LAST_ACCESS_TIME = "last_access_time";
    public static final String FIELD_EXPIRED = "expired";
    public static final String FIELD_HOST = "host";
    public static final String FIELD_ATTRIBUTES = "attributes";

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

    @JsonProperty(FIELD_ATTRIBUTES)
    public abstract Map<String, Object> attributes();

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

        @JsonProperty(FIELD_ATTRIBUTES)
        public abstract Builder attributes(Map<String, Object> attributes);

        public abstract SessionDTO build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SessionDTO.Builder();
        }
    }

    public static SessionDTO fromSimpleSession(SimpleSession session) {
        return fromSimpleSession(session, null);
    }

    public static SessionDTO fromSimpleSession(SimpleSession session, @Nullable String databaseId) {
        var builder = SessionDTO.builder()
                .expired(session.isExpired())
                .sessionId(session.getId().toString())
                .host(session.getHost())
                .timeout(session.getTimeout())
                .startTimestamp(session.getStartTimestamp().toInstant())
                .lastAccessTime(session.getLastAccessTime().toInstant())
                .attributes(session.getAttributeKeys().stream()
                        .collect(Collectors.toMap(Object::toString, session::getAttribute)));
        if (databaseId != null) {
            builder = builder.id(databaseId);
        }
        return builder.build();
    }

    public SimpleSession toSimpleSession() {
        final SimpleSession session = new SimpleSession();
        session.setId(sessionId());
        session.setHost(host());
        session.setTimeout(timeout());
        session.setStartTimestamp(Date.from(startTimestamp()));
        session.setLastAccessTime(Date.from(lastAccessTime()));
        session.setExpired(expired());
        session.setAttributes(Map.copyOf(attributes()));
        return session;
    }

    public Optional<String> userId() {
        // A subject can have more than one principal. If that's the case, the user ID is required to be the first one.
        return Optional.ofNullable(attributes().get(DefaultSubjectContext.PRINCIPALS_SESSION_KEY))
                .map(principals -> principals instanceof Iterable<?> iterable ? iterable : List.of(principals))
                .map(principals -> Iterables.getFirst(principals, null))
                .map(Object::toString);
    }
}
