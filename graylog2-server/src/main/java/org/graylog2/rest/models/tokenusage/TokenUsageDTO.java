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
package org.graylog2.rest.models.tokenusage;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@WithBeanGetter
public record TokenUsageDTO(
        @JsonProperty(FIELD_TOKEN_ID) String tokenId,
        @JsonProperty(FIELD_USERNAME) String username,
        @JsonProperty(FIELD_USER_ID) String userId,
        @JsonProperty(FIELD_TOKEN_NAME) String tokenName,
        @Nullable @JsonProperty(FIELD_CREATED_AT) DateTime createdAt,
        @JsonProperty(FIELD_LAST_ACCESS) DateTime lastAccess,
        @Nullable @JsonProperty(FIELD_EXPIRES_AT) DateTime expiresAt,
        @JsonProperty(FIELD_USER_IS_EXTERNAL) boolean userIsExternal,
        @JsonProperty(FIELD_AUTH_BACKEND) String authBackend) {

    public static final String FIELD_TOKEN_ID = "_id";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_TOKEN_NAME = "NAME";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_LAST_ACCESS = "last_access";
    public static final String FIELD_EXPIRES_AT = "expires_at";
    public static final String FIELD_USER_IS_EXTERNAL = "external_user";
    public static final String FIELD_AUTH_BACKEND = "title";

    public static TokenUsageDTO create(@JsonProperty(FIELD_TOKEN_ID) String tokenId,
                                       @JsonProperty(FIELD_USERNAME) String username,
                                       @JsonProperty(FIELD_USER_ID) String userId,
                                       @JsonProperty(FIELD_TOKEN_NAME) String tokenName,
                                       @Nullable @JsonProperty(FIELD_CREATED_AT) DateTime createdAt,
                                       @JsonProperty(FIELD_LAST_ACCESS) DateTime lastAccess,
                                       @Nullable @JsonProperty(FIELD_EXPIRES_AT) DateTime expiresAt,
                                       @JsonProperty(FIELD_USER_IS_EXTERNAL) boolean userIsExternal,
                                       @Nullable @JsonProperty(FIELD_AUTH_BACKEND) String authBackend) {
        return new TokenUsageDTO(tokenId, username, userId,
                tokenName,
                createdAt,
                lastAccess,
                expiresAt,
                userIsExternal,
                authBackend);
    }

}
