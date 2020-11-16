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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.util.LifecycleUtils;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SessionCreatorTest {
    private static final long SESSION_TIMEOUT = Long.MAX_VALUE;
    private final ActorAwareUsernamePasswordToken validToken = new ActorAwareUsernamePasswordToken("username",
            "password");
    private final ActorAwareUsernamePasswordToken invalidToken = new ActorAwareUsernamePasswordToken("username", "wrong password");


    @Mock
    private UserService userService;

    @Mock
    private AuditEventSender auditEventSender;

    @InjectMocks
    private SessionCreator sessionCreator;

    private DefaultSecurityManager securityManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        SimpleAccountRealm realm = new SimpleAccountRealm();
        realm.addAccount(validToken.getUsername(), String.valueOf(validToken.getPassword()));

        // Set up a security manager like in DefaultSecurityManagerProvider
        securityManager = new DefaultSecurityManager(realm);
        FirstSuccessfulStrategy strategy = new ThrowingFirstSuccessfulStrategy();
        strategy.setStopAfterFirstSuccess(true);
        ((ModularRealmAuthenticator) securityManager.getAuthenticator()).setAuthenticationStrategy(strategy);
        SecurityUtils.setSecurityManager(securityManager);
    }

    @After
    public void tearDown() {
        try {
            LifecycleUtils.destroy(SecurityUtils.getSecurityManager());
            SecurityUtils.setSecurityManager(null);
            ThreadContext.unbindSubject();
        } catch (Exception e) {
            // OK, we don't care
        }
    }

    @Test
    public void validAuthToken() {
        setUpUserMock();

        assertFalse(SecurityUtils.getSubject().isAuthenticated());
        Optional<Session> session = sessionCreator.create(null, "host", validToken);
        assertTrue(session.isPresent());
        assertEquals(SESSION_TIMEOUT, session.get().getTimeout());
        assertTrue(SecurityUtils.getSubject().isAuthenticated());
        verify(auditEventSender).success(eq(AuditActor.user("username")), anyString(), anyMap());
    }

    @Test
    public void invalidAuthToken() {
        sessionCreator.create(null, "host", invalidToken);
        verify(auditEventSender).failure(eq(validToken.getActor()), anyString(), anyMap());
    }

    @Test
    public void extendSession() {
        setUpUserMock();

        // Create a session and store it.
        SimpleSession oldSession = new SimpleSession();
        ((DefaultSessionManager) securityManager.getSessionManager()).getSessionDAO().create(oldSession);
        String oldSessionId = oldSession.getId().toString();

        assertFalse(SecurityUtils.getSubject().isAuthenticated());
        Optional<Session> session = sessionCreator.create(oldSessionId, "host", validToken);
        assertTrue(session.isPresent());
        assertEquals(SESSION_TIMEOUT, session.get().getTimeout());
        assertEquals(oldSessionId, session.get().getId());
        assertTrue(SecurityUtils.getSubject().isAuthenticated());
        verify(auditEventSender).success(eq(AuditActor.user("username")), anyString(), anyMap());
    }

    @Test
    public void extendExpiredSession() {
        setUpUserMock();

        // Create an expired session and store it.
        SimpleSession oldSession = new SimpleSession();
        oldSession.setLastAccessTime(new Date(0));
        ((DefaultSessionManager) securityManager.getSessionManager()).getSessionDAO().create(oldSession);
        String oldSessionId = oldSession.getId().toString();

        assertFalse(SecurityUtils.getSubject().isAuthenticated());
        Optional<Session> session = sessionCreator.create(oldSessionId, "host", validToken);
        assertTrue(session.isPresent());

        // User will get a new session
        assertNotEquals(oldSessionId, session.get().getId());

        assertTrue(SecurityUtils.getSubject().isAuthenticated());
    }

    @Test
    public void throwingRealmDoesNotInhibitAuthentication() {
        setUpUserMock();

        assertFalse(SecurityUtils.getSubject().isAuthenticated());

        // Put a throwing realm in the first position. Authentication should still be successful, because the second
        // realm will find an account for the user
        final List<Realm> realms = new ArrayList<>(securityManager.getRealms());
        realms.add(0, throwingRealm());
        securityManager.setRealms(realms);

        assertThat(sessionCreator.create(null, "host", validToken)).isPresent();
        assertThat(SecurityUtils.getSubject().isAuthenticated()).isTrue();
        verify(auditEventSender).success(eq(AuditActor.user("username")), anyString(), anyMap());
    }

    @Test
    public void serviceUnavailable() {
        setUpUserMock();

        assertFalse(SecurityUtils.getSubject().isAuthenticated());

        // First realm will throw, second realm will be unable to authenticate because user has no account
        securityManager.setRealms(ImmutableList.of(throwingRealm(), new SimpleAccountRealm()));

        assertThatThrownBy(() -> sessionCreator.create(null, "host", validToken)).isInstanceOf(
                AuthenticationServiceUnavailableException.class);
        assertThat(SecurityUtils.getSubject().isAuthenticated()).isFalse();
        verify(auditEventSender).failure(eq(AuditActor.user("username")), anyString(),
                argThat(map -> StringUtils.containsIgnoreCase((String) map.get("message"), "unavailable")));
    }

    /**
     * Test that the service unavailable exception is cleared when the service becomes available again
     */
    @Test
    public void serviceUnavailableStateIsCleared() {
        setUpUserMock();

        assertFalse(SecurityUtils.getSubject().isAuthenticated());

        final AtomicBoolean doThrow = new AtomicBoolean(true);
        final SimpleAccountRealm switchableRealm = new SimpleAccountRealm() {
            @Override
            protected AuthenticationInfo doGetAuthenticationInfo(
                    AuthenticationToken token) throws AuthenticationException {
                if (doThrow.get()) {
                    throw new AuthenticationServiceUnavailableException("not available");
                } else {
                    return super.doGetAuthenticationInfo(token);
                }
            }
        };

        securityManager.setRealms(ImmutableList.of(switchableRealm, new SimpleAccountRealm()));

        // realm will throw an exception on auth attempt
        assertThatThrownBy(() -> sessionCreator.create(null, "host", validToken)).isInstanceOf(
                AuthenticationServiceUnavailableException.class);
        assertThat(SecurityUtils.getSubject().isAuthenticated()).isFalse();

        // switch realm to not throw an exception but simply reject the credentials
        doThrow.set(false);

        sessionCreator.create(null, "host", validToken);
        assertThat(SecurityUtils.getSubject().isAuthenticated()).isFalse();
    }

    private void setUpUserMock() {
        User user = mock(User.class);
        when(user.getName()).thenReturn("username");
        when(user.getSessionTimeoutMs()).thenReturn(SESSION_TIMEOUT);
        when(userService.load("username")).thenReturn(user);
        when(userService.loadById("username")).thenReturn(user);
    }

    @Nonnull
    private SimpleAccountRealm throwingRealm() {
        return new SimpleAccountRealm() {
            @Override
            protected AuthenticationInfo doGetAuthenticationInfo(
                    AuthenticationToken token) throws AuthenticationException {
                throw new AuthenticationServiceUnavailableException("not available");
            }
        };
    }
}
