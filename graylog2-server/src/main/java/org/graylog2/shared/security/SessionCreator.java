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

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.rest.models.system.sessions.SessionUtils;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

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
     * Attempts to log the user in with the given authentication token and returns a new session upon
     * success.
     * <p>
     * Side effect: the user will be registered with the current security context.
     *
     * @param host      Host the request to create a session originates from.
     * @param authToken Authentication token to log the user in.
     * @return A session for the authenticated user wrapped in an {@link Optional}, or an empty {@link Optional} if
     * authentication failed.
     * @throws AuthenticationServiceUnavailableException If authenticating the user fails not due to an issue with the
     *                                                   credentials but because of an external resource being
     *                                                   unavailable
     */
    public Optional<Session> login(String host, ActorAwareAuthenticationToken authToken)
            throws AuthenticationServiceUnavailableException {

        final Subject subject = new Subject.Builder().host(host).buildSubject();

        ThreadContext.bind(subject);

        try {
            subject.login(authToken);
            return Optional.of(createForSubject(subject));
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

    /**
     * Creates a session for the given user.
     *
     * @param subject The subject that should be associated with the session.
     * @return A session for the given subject. Depending on the subject's authentication state,
     * the session state may be unauthenticated.
     */
    public Session createForSubject(Subject subject) {
        final var session = subject.getSession();

        final var userId = subject.getPrincipal().toString();
        final var user = userService.loadById(userId);

        if (user != null) {
            long timeoutInMillis = user.getSessionTimeoutMs();
            session.setTimeout(timeoutInMillis);
            session.setAttribute(SessionUtils.USERNAME_SESSION_KEY, user.getName());
        } else {
            session.setTimeout(UserConfiguration.DEFAULT_VALUES.globalSessionTimeoutInterval().toMillis());
        }
        session.touch();

        // Save subject state in session. This will save the principal (i.e. user id) as well as the authentication
        // state of the subject in the session.
        ((DefaultSecurityManager) SecurityUtils.getSecurityManager()).getSubjectDAO().save(subject);

        final Map<String, Object> auditEventContext = ImmutableMap.of(
                "session_id", session.getId(),
                "remote_address", Optional.ofNullable(session.getHost()).orElse("n/a")
        );
        auditEventSender.success(AuditActor.user(user.getName()), SESSION_CREATE, auditEventContext);

        return session;
    }
}
