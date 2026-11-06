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
package org.graylog.mcp.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;
import org.graylog2.jackson.MongoInstantDeserializer;
import org.graylog2.jackson.MongoInstantSerializer;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;

/**
 * A refresh token. Refresh tokens are rotated on every use, all tokens descending from the same authorization share
 * a {@code familyId}.
 *
 * @param accessTokenId        the access token issued together with this refresh token
 * @param accessTokenExpiresAt used to tell an expired access token from a revoked one
 * @param used                 whether this token was already exchanged. Presenting a used token revokes its family.
 */
public record OAuthRefreshToken(
        @ObjectId @Id @Nullable @JsonProperty(MongoEntity.FIELD_ID) String id,
        @JsonProperty(FIELD_TOKEN_HASH) String tokenHash,
        @JsonProperty(FIELD_FAMILY_ID) String familyId,
        @JsonProperty(FIELD_CLIENT_ID) String clientId,
        @JsonProperty(FIELD_USER_ID) String userId,
        @JsonProperty(FIELD_USERNAME) String username,
        @JsonProperty(FIELD_ACCESS_TOKEN_ID) String accessTokenId,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_ACCESS_TOKEN_EXPIRES_AT) Instant accessTokenExpiresAt,
        @Nullable @JsonProperty(FIELD_RESOURCE) String resource,
        @JsonProperty(FIELD_USED) boolean used,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_CREATED_AT) Instant createdAt,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_EXPIRES_AT) Instant expiresAt
) implements MongoEntity {
    public static final String FIELD_TOKEN_HASH = "token_hash";
    public static final String FIELD_FAMILY_ID = "family_id";
    public static final String FIELD_CLIENT_ID = "client_id";
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_ACCESS_TOKEN_ID = "access_token_id";
    public static final String FIELD_ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at";
    public static final String FIELD_RESOURCE = "resource";
    public static final String FIELD_USED = "used";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_EXPIRES_AT = "expires_at";
}
