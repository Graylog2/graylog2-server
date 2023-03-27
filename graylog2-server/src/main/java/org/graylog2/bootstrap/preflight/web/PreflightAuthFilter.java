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
import org.graylog2.Configuration;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringTokenizer;

public class PreflightAuthFilter implements ContainerRequestFilter {

    private static final String AUTHORIZATION_PROPERTY = "Authorization";
    private static final String AUTHENTICATION_SCHEME = "Basic";
    private final String adminUsername;
    private final String adminPasswordHash;

    @Inject
    public PreflightAuthFilter(Configuration configuration) {
        this.adminUsername = configuration.getRootUsername();
        this.adminPasswordHash = configuration.getRootPasswordSha2();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();

        final List<String> authorization = headers.get(AUTHORIZATION_PROPERTY);

        if (authorization == null || authorization.isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You cannot access this resource")
                    .header("WWW-Authenticate", "Basic realm=preflight-config")
                    .build());
            return;
        }

        final String encodedUserPassword = authorization.get(0).replaceFirst(AUTHENTICATION_SCHEME + " ", "");

        String usernameAndPassword = new String(Base64.decode(encodedUserPassword.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
        final String username = tokenizer.nextToken();
        final String password = tokenizer.nextToken();

        if (!isUserMatching(username, password)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You cannot access this resource")
                    .header("WWW-Authenticate", "Basic realm=preflight-config")
                    .build());
        }
    }

    private boolean isUserMatching(String username, String password) {
        return username.equals(this.adminUsername) && isPasswordMatching(password);
    }

    private boolean isPasswordMatching(String password) {
        return DigestUtils.sha256Hex(password).equals(adminPasswordHash);
    }
}
