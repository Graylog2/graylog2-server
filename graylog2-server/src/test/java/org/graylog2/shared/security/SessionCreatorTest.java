/*
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.security;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.util.LifecycleUtils;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SessionCreatorTest {
    private static final long SESSION_TIMEOUT = 1234L;
    private final UsernamePasswordToken validToken = new UsernamePasswordToken("username", "password");
    private final UsernamePasswordToken invalidToken = new UsernamePasswordToken("username", "wrong password");

    @Mock
    private UserService userService;

    @InjectMocks
    private SessionCreator sessionCreator;

    private DefaultSecurityManager securityManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        SimpleAccountRealm realm = new SimpleAccountRealm();
        realm.addAccount(validToken.getUsername(), String.valueOf(validToken.getPassword()));
        securityManager = new DefaultSecurityManager(realm);
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
        Session session = sessionCreator.create(null, "host", validToken);
        assertNotNull(session);
        assertEquals(1234L, session.getTimeout());
        assertTrue(SecurityUtils.getSubject().isAuthenticated());
    }

    @Test(expected = AuthenticationException.class)
    public void invalidAuthToken() {
        sessionCreator.create(null, "host", invalidToken);
    }

    @Test
    public void extendSession() {
        setUpUserMock();

        // Create a session and store it.
        SimpleSession oldSession = new SimpleSession();
        ((DefaultSessionManager) securityManager.getSessionManager()).getSessionDAO().create(oldSession);
        String oldSessionId = oldSession.getId().toString();

        assertFalse(SecurityUtils.getSubject().isAuthenticated());
        Session session = sessionCreator.create(oldSessionId, "host", validToken);
        assertNotNull(session);
        assertEquals(SESSION_TIMEOUT, session.getTimeout());
        assertEquals(oldSessionId, session.getId());
        assertTrue(SecurityUtils.getSubject().isAuthenticated());
    }

    private void setUpUserMock() {
        User user = mock(User.class);
        when(user.getSessionTimeoutMs()).thenReturn(SESSION_TIMEOUT);
        when(userService.load("username")).thenReturn(user);
    }
}
