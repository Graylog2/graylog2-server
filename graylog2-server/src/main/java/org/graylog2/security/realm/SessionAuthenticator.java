/**
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
import org.graylog2.plugin.database.users.User;
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

    @Inject
    SessionAuthenticator(UserService userService) {
        this.userService = userService;
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

        final Optional<String> noSessionExtension = ShiroRequestHeadersBinder.getHeaderFromThreadContext(X_GRAYLOG_NO_SESSION_EXTENSION);
        if (noSessionExtension.isPresent() && "true".equalsIgnoreCase(noSessionExtension.get())) {
            LOG.debug("Not extending session because the request indicated not to.");
        } else {
            session.touch();
        }
        ThreadContext.bind(subject);

        return new SimpleAccount(user.getId(), null, "session authenticator");
    }
}
