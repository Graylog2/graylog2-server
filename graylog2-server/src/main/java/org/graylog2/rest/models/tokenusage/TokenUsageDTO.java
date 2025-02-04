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
        @JsonProperty("token_id") String tokenId,
        @JsonProperty("username") String username,
        @JsonProperty("user_id") String userId,
        @JsonProperty("token_name") String tokenName,
        @Nullable @JsonProperty("created_at") DateTime createdAt,
        @JsonProperty("last_access") DateTime lastAccess,
        @JsonProperty("user_is_external") boolean userIsExternal,
        @JsonProperty("auth_backend") String authBackend) {

    public static TokenUsageDTO create(@JsonProperty("token_id") String tokenId,
                                       @JsonProperty("username") String username,
                                       @JsonProperty("user_id") String userId,
                                       @JsonProperty("token_name") String tokenName,
                                       @Nullable @JsonProperty("created_at") DateTime createdAt,
                                       @JsonProperty("last_access") DateTime lastAccess,
                                       @JsonProperty("user_is_external") boolean userIsExternal,
                                       @Nullable @JsonProperty("auth_backend") String authBackend) {
        return new TokenUsageDTO(tokenId, username, userId,
                tokenName,
                createdAt,
                lastAccess,
                userIsExternal,
                authBackend);
    }

}
