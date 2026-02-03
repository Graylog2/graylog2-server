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
package org.graylog2.rest.models.system.sessions.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public interface SessionResponse {
    @JsonProperty("valid_until")
    Date validUntil();

    @JsonProperty("session_id")
    String sessionId();

    @JsonProperty("username")
    String username();

    @JsonProperty("user_id")
    String userId();

    interface Builder<T extends SessionResponse, B extends Builder<T, B>> {
        B validUntil(Date validUntil);

        B sessionId(String sessionId);

        B username(String username);

        B userId(String userId);

        T build();
    }
}
