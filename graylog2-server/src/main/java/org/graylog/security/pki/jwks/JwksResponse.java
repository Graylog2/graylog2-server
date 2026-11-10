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
package org.graylog.security.pki.jwks;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * JSON Web Key Set (JWKS) response as defined in RFC 7517.
 *
 * @param keys the list of JSON Web Keys
 */
public record JwksResponse(
        @JsonProperty("keys") List<Jwk> keys
) {

    /**
     * Creates an empty JWKS response.
     *
     * @return an empty JWKS response
     */
    public static JwksResponse empty() {
        return new JwksResponse(List.of());
    }
}
