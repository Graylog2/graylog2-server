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
package org.graylog.grpc.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.shared.security.AccessTokenAuthToken;
import org.graylog2.shared.security.ShiroSecurityContext;

/**
 * Authenticator used for getting a shiro subject for an access token in a gRPC context
 */
public class ShiroAuthenticator {
    private final DefaultSecurityManager securityManager;

    @Inject
    public ShiroAuthenticator(DefaultSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public Subject authenticate(String host, String token) {
        try {
            final AccessTokenAuthToken authToken = new AccessTokenAuthToken(token, host);
            final Subject subject = new Subject.Builder(securityManager).host(host)
                    .sessionCreationEnabled(false)
                    .buildSubject();
            final ShiroSecurityContext shiroContext =
                    new ShiroSecurityContext(subject, authToken, true, null, new MultivaluedHashMap<>());
            shiroContext.loginSubject();
            return subject;
        } finally {
            // We don't need any subject-related data in the shiro thread context so we clean it up immediately
            ThreadContext.unbindSubject();
        }
    }
}
