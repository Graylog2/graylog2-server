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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AccessTokenEntity.Builder.class)
public abstract class AccessTokenEntity implements MongoEntity {

    public static final String FIELD_ID = "_id";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_NAME = "NAME";

    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_LAST_ACCESS = "last_access";
    public static final String FIELD_EXPIRES_AT = "expires_at";

    @Id
    @ObjectId
    @Nullable
    @Override
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_USERNAME)
    public abstract String userName();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @Nullable
    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    @Nullable
    @JsonProperty(FIELD_LAST_ACCESS)
    public abstract DateTime lastAccess();

    @Nullable
    @JsonProperty(FIELD_EXPIRES_AT)
    public abstract DateTime expiresAt();

    @AutoValue.Builder
    @JsonIgnoreProperties({"token", "token_type"})
    public abstract static class Builder {

        @JsonCreator
        public static AccessTokenEntity.Builder create() {
            return new AutoValue_AccessTokenEntity.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_USERNAME)
        public abstract Builder userName(String username);

        @JsonProperty(FIELD_NAME)
        public abstract Builder name(String name);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(@Nullable DateTime createdAt);

        @JsonProperty(FIELD_LAST_ACCESS)
        public abstract Builder lastAccess(@Nullable DateTime lastAccess);

        @JsonProperty(FIELD_EXPIRES_AT)
        public abstract Builder expiresAt(@Nullable DateTime expiresAt);

        public abstract AccessTokenEntity build();
    }
}
