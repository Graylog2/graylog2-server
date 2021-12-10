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
package org.graylog2.security.realm;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.HttpHeadersToken;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Cookie;

public class CookieAuthenticationRealm extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(CookieAuthenticationRealm.class);

    public static final String NAME = "cookie-authentication";
    public static final String SESSION_COOKIE_NAME = "authentication";

    private final UserService userService;

    @Inject
    public CookieAuthenticationRealm(UserService userService) {
        this.userService = userService;

        setAuthenticationTokenClass(HttpHeadersToken.class);
        setCachingEnabled(false);
        // Credentials will be matched via the authentication service itself so we don't need Shiro to do it
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        final HttpHeadersToken headersToken = (HttpHeadersToken) authenticationToken;
        final Cookie authenticationCookie = headersToken.getCookies().get(SESSION_COOKIE_NAME);

        final Subject subject = new Subject.Builder().sessionId(authenticationCookie.getValue()).buildSubject();
        final Session session = subject.getSession(false);
        if (session == null) {
            LOG.debug("Invalid session. Either it has expired or did not exist.");
            return null;
        }

        final Object userId = subject.getPrincipal();
        final User user = userService.loadById(String.valueOf(userId));
        if (user == null) {
            LOG.debug("No user with userId {} found for session", userId);
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found session for userId {}", userId);
        }

        return new SimpleAccount(user.getId(), null, "session authenticator");
    }
}
