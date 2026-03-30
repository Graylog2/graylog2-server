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

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog2.shared.rest.NonApiResource;

/**
 * REST resource exposing the JWKS (JSON Web Key Set) endpoint.
 * <p>
 * This endpoint provides public keys for signature verification in standard JWKS format,
 * allowing external services and agents to validate tokens signed by Graylog.
 * <p>
 * The endpoint is unauthenticated because:
 * <ul>
 *   <li>It only exposes public keys, which are not sensitive</li>
 *   <li>Agents need to validate enrollment tokens before they are authenticated</li>
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7517">RFC 7517 - JSON Web Key (JWK)</a>
 */
@NonApiResource(prefix = "/.well-known")
@Path("/jwks")
public class JwksResource {

    private final JwksService jwksService;

    @Inject
    public JwksResource(JwksService jwksService) {
        this.jwksService = jwksService;
    }

    /**
     * Returns the JWKS containing all valid signing keys.
     * <p>
     * Only keys that are currently valid and have digitalSignature key usage
     * are included in the response.
     *
     * @return the JWKS response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JwksResponse getJwks() {
        return jwksService.getJwks();
    }
}
