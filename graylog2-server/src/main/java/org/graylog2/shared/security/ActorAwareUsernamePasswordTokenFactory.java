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
package org.graylog2.shared.security;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ActorAwareUsernamePasswordTokenFactory implements ActorAwareAuthenticationTokenFactory {

    @Override
    public ActorAwareAuthenticationToken forRequestBody(JsonNode jsonBody) {

        String missingProperties = Stream.of("username", "password")
                .filter(key -> jsonBody.get(key) == null || jsonBody.get(key).asText().isEmpty())
                .collect(Collectors.joining(", "));

        if (!missingProperties.isEmpty()) {
            throw new IllegalArgumentException("Missing required properties: " + missingProperties + ".");
        }

        String username = jsonBody.get("username").asText();
        String password = jsonBody.get("password").asText();

        return new ActorAwareUsernamePasswordToken(username, password);
    }
}
