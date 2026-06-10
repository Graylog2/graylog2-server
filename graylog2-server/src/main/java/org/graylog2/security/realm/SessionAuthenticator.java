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

import jakarta.inject.Inject;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.system.sessions.SessionUtils;
import org.graylog2.security.headerauth.HTTPHeaderAuthConfig;
import org.graylog2.shared.security.SessionIdToken;
import org.graylog2.shared.security.ShiroRequestHeadersBinder;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public class SessionAuthenticator extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(SessionAuthenticator.class);
    public static final String NAME = "mongodb-session";
    public static final String X_GRAYLOG_NO_SESSION_EXTENSION = "X-Graylog-No-Session-Extension";
    private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(1);

    private final UserService userService;
    private final ClusterConfigService clusterConfigService;
    private final Clock clock;

    @Inject
    SessionAuthenticator(UserService userService, ClusterConfigService clusterConfigService, Clock clock) {
        this.userService = userService;
        this.clusterConfigService = clusterConfigService;
        this.clock = clock;
        // this realm either rejects a session, or allows the associated user implicitly
        setCredentialsMatcher(new ServiceValidatedCredentialsMatcher());
        setAuthenticationTokenClass(SessionIdToken.class);
        setCachingEnabled(false);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        SessionIdToken sessionIdToken = (SessionIdToken) token;
        final Subject subject = new Subject.Builder().sessionId(sessionIdToken.getSessionId()).buildSubject();

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

        // If trusted header authentication is enabled, ensure that the username in the session still matches the
        // username in the header. If there is a mismatch, the session will be terminated.
        final HTTPHeaderAuthConfig httpHeaderConfig = loadHTTPHeaderConfig();
        if (httpHeaderConfig.enabled()) {
            final String sessionUsername = (String) session.getAttribute(SessionUtils.USERNAME_SESSION_KEY);
            final Optional<String> usernameHeader = ShiroRequestHeadersBinder.getHeaderFromThreadContext(httpHeaderConfig.usernameHeader());
            if (usernameHeader.isPresent() && !usernameHeader.get().equalsIgnoreCase(sessionUsername)) {
                LOG.warn("Terminating session where user <{}> does not match trusted HTTP header <{}>.", sessionUsername, usernameHeader.get());
                session.stop();
                return null;
            }
        }

        final Optional<String> noSessionExtension = ShiroRequestHeadersBinder.getHeaderFromThreadContext(X_GRAYLOG_NO_SESSION_EXTENSION);
        if (noSessionExtension.isPresent() && "true".equalsIgnoreCase(noSessionExtension.get())) {
            LOG.debug("Not extending session because the request indicated not to.");
        } else if (shouldTouch(session)) {
            session.touch();
        }
        ThreadContext.bind(subject);

        return new SimpleAccount(user.getId(), ServiceValidatedCredentialsMatcher.AUTHENTICATED, "session authenticator");
    }

    private boolean shouldTouch(Session session) {
        final Date lastAccessTime = session.getLastAccessTime();
        if (lastAccessTime == null) {
            return true;
        }
        final var elapsed = Duration.between(lastAccessTime.toInstant(), Instant.now(clock));
        // A negative elapsed time indicates clock skew between cluster nodes.
        // Treat that as stale so we touch the session and correct the timestamp to local time.
        if (elapsed.isNegative()) {
            LOG.warn("Session last access time is in the future (by {}). This may indicate clock skew between cluster nodes.", elapsed.abs());
            return true;
        }
        return elapsed.compareTo(TOUCH_INTERVAL) >= 0;
    }

    private HTTPHeaderAuthConfig loadHTTPHeaderConfig() {
        return clusterConfigService.getOrDefault(HTTPHeaderAuthConfig.class, HTTPHeaderAuthConfig.createDisabled());
    }
}
