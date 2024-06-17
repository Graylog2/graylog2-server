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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class BasicAuthFilter implements ContainerRequestFilter {

    private static final String AUTHORIZATION_PROPERTY = "Authorization";
    private static final String AUTHENTICATION_SCHEME = "Basic";
    private final String adminUsername;
    private final String adminPasswordHash;
    private final String realm;
    private final URI loginPage;


    public BasicAuthFilter(String adminUsername, String adminPasswordHash, String realm, URI loginPage) {
        this.adminUsername = adminUsername;
        this.adminPasswordHash = adminPasswordHash;
        this.realm = realm;

        this.loginPage = loginPage;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();

        if (!hasValidCrendentials(headers) && !isLoginScreen(requestContext)) {
            abortRequestUnauthorized(requestContext);
        }
    }

    private boolean isLoginScreen(ContainerRequestContext requestContext) {
        final String path = requestContext.getUriInfo().getPath();
        // the only url that doesn't require basic auth
        return path.equals(StringUtils.stripStart(loginPage.getPath(), "/"));
    }


    private boolean hasValidCrendentials(MultivaluedMap<String, String> headers) {
        final List<String> authorization = headers.get(AUTHORIZATION_PROPERTY);
        if (authorization == null || authorization.isEmpty()) {
            return false;
        }

        final String encodedUserPassword = authorization.get(0).replaceFirst(AUTHENTICATION_SCHEME + " ", "");
        String usernameAndPassword = new String(Base64.decode(encodedUserPassword.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        final String[] parts = usernameAndPassword.split(":");

        if (parts.length != 2) {
            return false;
        }

        final String username = parts[0];
        final String password = parts[1];

        return isUserMatching(username, password);
    }

    private void abortRequestUnauthorized(ContainerRequestContext requestContext) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED) //(=moved temporarily)
                .location(loginPage)
                .entity("You cannot access this resource, missing or invalid authorization header!")
                .header("WWW-Authenticate", "Basic realm=" + this.realm)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build());
    }

    private boolean isUserMatching(String username, String password) {
        return username.equals(this.adminUsername) && isPasswordMatching(password);
    }

    private boolean isPasswordMatching(String password) {
        return DigestUtils.sha256Hex(password).equals(adminPasswordHash);
    }
}
