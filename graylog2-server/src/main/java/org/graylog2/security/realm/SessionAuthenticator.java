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
import org.apache.shiro.util.ThreadContext;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.headerauth.HTTPHeaderAuthConfig;
import org.graylog2.shared.security.SessionIdToken;
import org.graylog2.shared.security.ShiroRequestHeadersBinder;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class SessionAuthenticator extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(SessionAuthenticator.class);
    public static final String NAME = "mongodb-session";
    public static final String X_GRAYLOG_NO_SESSION_EXTENSION = "X-Graylog-No-Session-Extension";

    private final UserService userService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    SessionAuthenticator(UserService userService, ClusterConfigService clusterConfigService) {
        this.userService = userService;
        this.clusterConfigService = clusterConfigService;
        // this realm either rejects a session, or allows the associated user implicitly
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
        setAuthenticationTokenClass(SessionIdToken.class);
        setCachingEnabled(false);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        SessionIdToken sessionIdToken = (SessionIdToken) token;
        final Subject subject = new Subject.Builder().sessionId(sessionIdToken.getSessionId()).buildSubject();
        final Session session = subject.getSession(false);
        if (session == null) {
            LOG.debug("Invalid session {}. Either it has expired or did not exist.", sessionIdToken.getSessionId());
            return null;
        }

        final Object userId = subject.getPrincipal();
        final User user = userService.loadById(String.valueOf(userId));
        if (user == null) {
            LOG.debug("No user with userId {} found for session {}", userId, sessionIdToken.getSessionId());
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found session {} for userId {}", session.getId(), userId);
        }

        final String sessionUsername = (String) session.getAttribute(HTTPHeaderAuthenticationRealm.SESSION_AUTH_HEADER);
        if (sessionUsername != null) {
            final HTTPHeaderAuthConfig httpHeaderConfig = loadHTTPHeaderConfig();
            final Optional<String> usernameHeader = ShiroRequestHeadersBinder.getHeaderFromThreadContext(httpHeaderConfig.usernameHeader());

            if (httpHeaderConfig.enabled() && usernameHeader.isPresent() && !usernameHeader.get().equalsIgnoreCase(sessionUsername)) {
                LOG.warn("Terminating session where user <{}> does not match trusted HTTP header <{}>.", sessionUsername, usernameHeader.get());
                session.stop();
                return null;
            }
        }

        final Optional<String> noSessionExtension = ShiroRequestHeadersBinder.getHeaderFromThreadContext(X_GRAYLOG_NO_SESSION_EXTENSION);
        if (noSessionExtension.isPresent() && "true".equalsIgnoreCase(noSessionExtension.get())) {
            LOG.debug("Not extending session because the request indicated not to.");
        } else {
            session.touch();
        }
        ThreadContext.bind(subject);

        return new SimpleAccount(user.getId(), null, "session authenticator");
    }

    private HTTPHeaderAuthConfig loadHTTPHeaderConfig() {
        return clusterConfigService.getOrDefault(HTTPHeaderAuthConfig.class, HTTPHeaderAuthConfig.createDisabled());
    }
}
