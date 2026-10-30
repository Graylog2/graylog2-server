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
import java.util.List;

/**
 * An OAuth client registered via dynamic client registration (RFC 7591).
 *
 * @param expiresAt the client is removed after this point in time. It is pushed forward whenever tokens are issued.
 */
public record OAuthClient(
        @ObjectId @Id @Nullable @JsonProperty(MongoEntity.FIELD_ID) String id,
        @JsonProperty(FIELD_CLIENT_ID) String clientId,
        @JsonProperty(FIELD_CLIENT_NAME) String clientName,
        @Nullable @JsonProperty(FIELD_CLIENT_URI) String clientUri,
        @JsonProperty(FIELD_REDIRECT_URIS) List<String> redirectUris,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_CREATED_AT) Instant createdAt,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_EXPIRES_AT) Instant expiresAt
) implements MongoEntity {
    public static final String FIELD_CLIENT_ID = "client_id";
    public static final String FIELD_CLIENT_NAME = "client_name";
    public static final String FIELD_CLIENT_URI = "client_uri";
    public static final String FIELD_REDIRECT_URIS = "redirect_uris";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_EXPIRES_AT = "expires_at";
}
