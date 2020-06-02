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
package org.graylog2.shared.security;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.graylog2.audit.AuditEventTypes.SESSION_CREATE;

public class SessionCreator {
    private static final Logger log = LoggerFactory.getLogger(SessionCreator.class);

    private final UserService userService;
    private final AuditEventSender auditEventSender;

    @Inject
    public SessionCreator(UserService userService, AuditEventSender auditEventSender) {
        this.userService = userService;
        this.auditEventSender = auditEventSender;
    }

    /**
     * Attempts to log the user in with the given authentication token and returns a new or renewed session upon
     * success.
     * <p>
     * Side effect: the user will be registered with the current security context.
     *
     * @param currentSessionId A session id, if one exists currently.
     * @param host Host the request to create a session originates from.
     * @param authToken Authentication token to log the user in.
     * @return A session for the authenticated user wrapped in an {@link Optional}, or an empty {@link Optional} if
     *         authentication failed.
     * @throws AuthenticationServiceUnavailableException If authenticating the user fails not due to an issue with the
     *                                                   credentials but because of an external resource being
     *                                                   unavailable
     */
    public Optional<Session> create(@Nullable String currentSessionId, String host,
            ActorAwareAuthenticationToken authToken) throws AuthenticationServiceUnavailableException {

        final String previousSessionId = StringUtils.defaultIfBlank(currentSessionId, null);
        final Subject subject = new Subject.Builder().sessionId(previousSessionId).host(host).buildSubject();

        ThreadContext.bind(subject);

        try {
            final Session session = subject.getSession();

            subject.login(authToken);

            String username = subject.getPrincipal().toString();
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

            final Map<String, Object> auditEventContext = ImmutableMap.of(
                    "session_id", session.getId(),
                    "remote_address", host
            );
            auditEventSender.success(AuditActor.user(username), SESSION_CREATE, auditEventContext);

            return Optional.of(session);
        } catch (AuthenticationServiceUnavailableException e) {
            log.info("Session creation failed due to authentication service being unavailable. Actor: \"{}\"",
                    authToken.getActor().urn());
            final Map<String, Object> auditEventContext = ImmutableMap.of(
                    "remote_address", host,
                    "message", "Authentication service unavailable: " + e.getMessage()
            );
            auditEventSender.failure(authToken.getActor(), SESSION_CREATE, auditEventContext);
            throw e;
        } catch (AuthenticationException e) {
            log.info("Invalid credentials in session create request. Actor: \"{}\"", authToken.getActor().urn());
            final Map<String, Object> auditEventContext = ImmutableMap.of(
                    "remote_address", host
            );
            auditEventSender.failure(authToken.getActor(), SESSION_CREATE, auditEventContext);
            return Optional.empty();
        }
    }
}
