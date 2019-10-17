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

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class SessionCreator {
    private static final Logger LOG = LoggerFactory.getLogger(SessionCreator.class);

    private final UserService userService;

    @Inject
    public SessionCreator(UserService userService) {
        this.userService = userService;
    }

    /**
     * Attempts to log the user in with the given authentication token and returns a new or renewed session upon
     * success.
     *
     * @param currentSessionId A session id, if one exists currently.
     * @param host Host the request to create a session originates from.
     * @param authToken Authentication token to log the user in with.
     * @return A session if the user was authenticated, null if authentication succeeded but
     * @throws AuthenticationException if authenticating the user fails
     * @throws UnknownSessionException if a session id was given but it is unknown
     */
    public Session create(@Nullable String currentSessionId, String host, AuthenticationToken authToken) {

        final String previousSessionId = StringUtils.defaultIfBlank(currentSessionId, null);
        final Subject subject = new Subject.Builder().sessionId(previousSessionId).host(host).buildSubject();

        ThreadContext.bind(subject);
        final Session session = subject.getSession();

        try {
            subject.login(authToken);

            String username = (String) subject.getPrincipal();
            final User user = userService.load(username);

            if (user != null) {
                long timeoutInMillis = user.getSessionTimeoutMs();
                session.setTimeout(timeoutInMillis);
            } else {
                // set a sane default. really we should be able to load the user from above.
                session.setTimeout(TimeUnit.HOURS.toMillis(8));
            }
            session.touch();

            // save subject in session, otherwise we can't get the username back in subsequent requests.
            ((DefaultSecurityManager) SecurityUtils.getSecurityManager()).getSubjectDAO().save(subject);

            return session;
        } catch (UnknownSessionException e) {
            subject.logout();
            throw (e);
        }
    }
}
