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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.lang.util.LifecycleUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.headerauth.HTTPHeaderAuthConfig;
import org.graylog2.shared.security.SessionIdToken;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticatorTest {

    private static final String TEST_USER_ID = "test-user-id";

    @Mock
    private UserService userService;

    @Mock
    private ClusterConfigService clusterConfigService;

    private SessionAuthenticator authenticator;
    private DefaultSecurityManager securityManager;

    @BeforeEach
    void setUp() {
        authenticator = new SessionAuthenticator(userService, clusterConfigService);

        final var realm = new SimpleAccountRealm();
        realm.addAccount(TEST_USER_ID, "password");

        securityManager = new DefaultSecurityManager(realm);
        SecurityUtils.setSecurityManager(securityManager);

        when(clusterConfigService.getOrDefault(HTTPHeaderAuthConfig.class, HTTPHeaderAuthConfig.createDisabled()))
                .thenReturn(HTTPHeaderAuthConfig.createDisabled());

        final var user = mock(User.class);
        when(user.getId()).thenReturn(TEST_USER_ID);
        when(userService.loadById(TEST_USER_ID)).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        try {
            LifecycleUtils.destroy(SecurityUtils.getSecurityManager());
        } catch (Exception ignored) {
        }
        SecurityUtils.setSecurityManager(null);
        ThreadContext.unbindSubject();
    }

    @Test
    void touchesSessionWhenLastAccessTimeIsStale() {
        final var sessionId = createSession();
        final var staleTime = Instant.now().minus(2, ChronoUnit.MINUTES);
        setLastAccessTime(sessionId, staleTime);

        final var lastAccessBefore = getSessionLastAccessTime(sessionId);

        authenticator.getAuthenticationInfo(new SessionIdToken(sessionId, "host", "addr"));

        final var lastAccessAfter = getSessionLastAccessTime(sessionId);
        assertThat(lastAccessAfter).isAfter(lastAccessBefore);
    }

    @Test
    void skipsTouchWhenLastAccessTimeIsRecent() {
        final var sessionId = createSession();

        final var lastAccessBefore = getSessionLastAccessTime(sessionId);

        authenticator.getAuthenticationInfo(new SessionIdToken(sessionId, "host", "addr"));

        final var lastAccessAfter = getSessionLastAccessTime(sessionId);
        assertThat(lastAccessAfter).isEqualTo(lastAccessBefore);
    }

    @Test
    void touchesSessionWhenLastAccessTimeIsExactlyAtThreshold() {
        final var sessionId = createSession();
        final var thresholdTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        setLastAccessTime(sessionId, thresholdTime);

        final var lastAccessBefore = getSessionLastAccessTime(sessionId);

        authenticator.getAuthenticationInfo(new SessionIdToken(sessionId, "host", "addr"));

        final var lastAccessAfter = getSessionLastAccessTime(sessionId);
        assertThat(lastAccessAfter).isAfter(lastAccessBefore);
    }

    private String createSession() {
        final var subject = new Subject.Builder().host("localhost").buildSubject();
        ThreadContext.bind(subject);
        subject.login(new UsernamePasswordToken(TEST_USER_ID, "password"));
        final var sessionId = subject.getSession().getId().toString();
        ThreadContext.unbindSubject();
        return sessionId;
    }

    private void setLastAccessTime(String sessionId, Instant time) {
        final var sessionManager = (DefaultSessionManager) securityManager.getSessionManager();
        final var session = (SimpleSession) sessionManager.getSessionDAO().readSession(sessionId);
        session.setLastAccessTime(Date.from(time));
    }

    private Date getSessionLastAccessTime(String sessionId) {
        final var sessionManager = (DefaultSessionManager) securityManager.getSessionManager();
        final var session = (SimpleSession) sessionManager.getSessionDAO().readSession(sessionId);
        return session.getLastAccessTime();
    }
}
