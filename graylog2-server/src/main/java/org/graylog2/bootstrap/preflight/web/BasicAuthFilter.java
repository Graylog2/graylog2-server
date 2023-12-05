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
package org.graylog2.bootstrap.preflight.web;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.util.encoders.Base64;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringTokenizer;

public class BasicAuthFilter implements ContainerRequestFilter {

    private static final String AUTHORIZATION_PROPERTY = "Authorization";
    private static final String AUTHENTICATION_SCHEME = "Basic";
    private final String adminUsername;
    private final String adminPasswordHash;
    private final String realm;


    public BasicAuthFilter(String adminUsername, String adminPasswordHash, String realm) {
        this.adminUsername = adminUsername;
        this.adminPasswordHash = adminPasswordHash;
        this.realm = realm;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();

        final List<String> authorization = headers.get(AUTHORIZATION_PROPERTY);

        if (authorization == null || authorization.isEmpty()) {
            abortRequestUnauthorized(requestContext, "You cannot access this resource, missing authorization header!");
            return;
        }

        final String encodedUserPassword = authorization.get(0).replaceFirst(AUTHENTICATION_SCHEME + " ", "");

        String usernameAndPassword = new String(Base64.decode(encodedUserPassword.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        final String[] parts = usernameAndPassword.split(":");

        if(parts.length != 2) {
            abortRequestUnauthorized(requestContext, "You cannot access this resource, invalid username/password combination!");
            return;
        }

        final String username = parts[0];
        final String password = parts[1];

        if (!isUserMatching(username, password)) {
            abortRequestUnauthorized(requestContext, "You cannot access this resource, invalid username/password combination!");
        }
    }

    private void abortRequestUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(message)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .header("WWW-Authenticate", "Basic realm=" + this.realm)
                .build());
    }

    private boolean isUserMatching(String username, String password) {
        return username.equals(this.adminUsername) && isPasswordMatching(password);
    }

    private boolean isPasswordMatching(String password) {
        return DigestUtils.sha256Hex(password).equals(adminPasswordHash);
    }
}
