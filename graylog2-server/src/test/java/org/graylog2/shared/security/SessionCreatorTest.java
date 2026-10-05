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
import com.google.common.eventbus.EventBus;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.lang.util.LifecycleUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.bindings.providers.DefaultSecurityManagerProvider;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.InMemoryRolePermissionResolver;
import org.graylog2.security.OrderedAuthenticatingRealms;
import org.graylog2.security.sessions.AuthenticationInfoWithSessionAuthContext;
import org.graylog2.security.sessions.SessionAuthContext;
import org.graylog2.security.sessions.SessionDAO;
import org.graylog2.security.sessions.SessionService;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.rest.models.system.sessions.SessionUtils.AUTH_CONTEXT_SESSION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SessionCreatorTest {
    private AutoCloseable mocks;
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

    @BeforeEach
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        SimpleAccountRealm realm = new SimpleAccountRealm();
        realm.addAccount(validToken.getUsername(), String.valueOf(validToken.getPassword()));

        // Build the SecurityManager through the production provider so the test reflects real wiring
        // (auth listeners, strategy, session manager, subject DAO) instead of duplicating it. A change to
        // DefaultSecurityManagerProvider that breaks session attribute persistence should fail tests here.
        securityManager = new DefaultSecurityManagerProvider(
                new SessionDAO(mock(SessionService.class), new EventBus()),
                Map.of(),
                mock(InMemoryRolePermissionResolver.class),
                new TestOrderedAuthenticatingRealms(List.of(realm))
        ).get();
    }

    @AfterEach
    public void tearDown() throws Exception {
        try {
            LifecycleUtils.destroy(SecurityUtils.getSecurityManager());
            SecurityUtils.setSecurityManager(null);
            ThreadContext.unbindSubject();
        } catch (Exception e) {
            // OK, we don't care
        }
        mocks.close();
    }

    @Test
    public void validAuthToken() {
        setUpUserMock();

        assertFalse(SecurityUtils.getSubject().isAuthenticated());
        Optional<Session> session = sessionCreator.login("host", validToken);
        assertTrue(session.isPresent());
        assertEquals(SESSION_TIMEOUT, session.get().getTimeout());
        assertTrue(SecurityUtils.getSubject().isAuthenticated());
        verify(auditEventSender).success(eq(AuditActor.user("username")), anyString(), anyMap());
    }

    @Test
    public void invalidAuthToken() {
        sessionCreator.login("host", invalidToken);
        verify(auditEventSender).failure(eq(validToken.getActor()), anyString(), anyMap());
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

        assertThat(sessionCreator.login("host", validToken)).isPresent();
        assertThat(SecurityUtils.getSubject().isAuthenticated()).isTrue();
        verify(auditEventSender).success(eq(AuditActor.user("username")), anyString(), anyMap());
    }

    @Test
    public void serviceUnavailable() {
        setUpUserMock();

        assertFalse(SecurityUtils.getSubject().isAuthenticated());

        // First realm will throw, second realm will be unable to authenticate because user has no account
        securityManager.setRealms(ImmutableList.of(throwingRealm(), new SimpleAccountRealm()));

        assertThatThrownBy(() -> sessionCreator.login("host", validToken)).isInstanceOf(
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
        assertThatThrownBy(() -> sessionCreator.login("host", validToken)).isInstanceOf(
                AuthenticationServiceUnavailableException.class);
        assertThat(SecurityUtils.getSubject().isAuthenticated()).isFalse();

        // switch realm to not throw an exception but simply reject the credentials
        doThrow.set(false);

        sessionCreator.login("host", validToken);
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

    /**
     * Login must persist a {@link SessionAuthContext} emitted by an authenticating realm onto the resulting session.
     * This is what SAML/OIDC backends rely on for logout (e.g. SAML SLO matching by SessionIndex).
     */
    @Test
    public void loginPersistsSessionAuthContextOnReturnedSession() {
        setUpUserMock();

        final SessionAuthContext expectedAuthContext = new TestSessionAuthContext("expected-value");
        securityManager.setRealms(ImmutableList.of(new AuthContextEmittingRealm(validToken, expectedAuthContext)));

        final Optional<Session> session = sessionCreator.login("host", validToken);

        assertThat(session).isPresent();
        assertThat(session.get().getAttribute(AUTH_CONTEXT_SESSION_KEY)).isEqualTo(expectedAuthContext);
    }

    private record TestSessionAuthContext(String value) implements SessionAuthContext {
        @Override
        public String type() {
            return "TEST";
        }
    }

    /**
     * Realm that wraps a successful authentication with a {@link SessionAuthContext}, like SAML/OIDC backends do.
     */
    private static final class AuthContextEmittingRealm extends SimpleAccountRealm {
        private final SessionAuthContext authContext;

        AuthContextEmittingRealm(ActorAwareUsernamePasswordToken validToken, SessionAuthContext authContext) {
            addAccount(validToken.getUsername(), String.valueOf(validToken.getPassword()));
            this.authContext = authContext;
        }

        @Override
        protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
            final AuthenticationInfo info = super.doGetAuthenticationInfo(token);
            return info == null ? null : new AuthenticationInfoWithSessionAuthContext(info, authContext);
        }
    }

    private static final class TestOrderedAuthenticatingRealms extends ArrayList<Realm> implements OrderedAuthenticatingRealms {
        TestOrderedAuthenticatingRealms(List<Realm> realms) {
            super(realms);
        }

        @Override
        public Optional<Realm> getRootAccountRealm() {
            return Optional.empty();
        }
    }
}
