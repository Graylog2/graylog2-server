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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.graylog2.rest.models.system.sessions.SessionUtils;
import org.graylog2.security.sessions.AuthenticationInfoWithSessionAuthContext;
import org.graylog2.security.sessions.SessionAuthContext;

/**
 * A listener that is invoked by shiro after an authentication attempt
 * (e.g. within {@link org.apache.shiro.subject.Subject#login(AuthenticationToken)}). The listener checks if the
 * authentication info contains a {@link SessionAuthContext} and if so, persists it to the current session.
 */
public class PersistSessionDataListener implements AuthenticationListener {
    @Override
    public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
        if (info instanceof AuthenticationInfoWithSessionAuthContext(
                AuthenticationInfo ignored, SessionAuthContext sessionAuthContext
        )) {
            final var session = SecurityUtils.getSubject().getSession(false);
            if (session != null) {
                session.setAttribute(SessionUtils.AUTH_CONTEXT_SESSION_KEY, sessionAuthContext);
            }
        }
    }

    @Override
    public void onFailure(AuthenticationToken token, AuthenticationException ae) {
        // No action needed on authentication failure
    }

    @Override
    public void onLogout(PrincipalCollection principals) {
        // No action needed on logout
    }
}
