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
package org.graylog2.security;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.events.UserChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Listens for {@link UserChangedEvent} events and terminates all sessions for disabled users.
 */
public class UserSessionTerminationListener {
    private static final Logger LOG = LoggerFactory.getLogger(UserSessionTerminationListener.class);

    // Sessions should be terminated when a user's account status is one of the following
    private static final EnumSet<User.AccountStatus> SESSION_TERMINATION_STATUS = EnumSet.of(
            User.AccountStatus.DELETED,
            User.AccountStatus.DISABLED
    );

    private final MongoDBSessionService sessionService;
    private final DefaultSecurityManager securityManager;
    private final UserService userService;

    @Inject
    public UserSessionTerminationListener(MongoDBSessionService sessionService,
                                          DefaultSecurityManager securityManager,
                                          UserService userService,
                                          EventBus eventBus) {
        this.sessionService = sessionService;
        this.securityManager = securityManager;
        this.userService = userService;

        eventBus.register(this);
    }

    @Subscribe
    public void handleUserChanged(UserChangedEvent event) {
        final User user = userService.loadById(event.userId());

        if (user != null && SESSION_TERMINATION_STATUS.contains(user.getAccountStatus())) {
            terminateSessionsFor(user);
        }
    }

    private void terminateSessionsFor(User user) {
        try {
            final Set<String> sessionIds = getSessionIDsForUser(user);
            final SessionManager sessionManager = securityManager.getSessionManager();

            for (final String sessionId : sessionIds) {
                final Session session = sessionManager.getSession(new DefaultSessionKey(sessionId));
                if (session != null) {
                    LOG.info("Stopping session <{}> for user <{}/{}>", sessionId, user.getName(), user.getId());
                    session.stop();
                }
            }
        } catch (Exception e) {
            LOG.error("Couldn't stop session for user <{}/{}>", user.getName(), user.getId(), e);
        }
    }

    private Set<String> getSessionIDsForUser(User user) {
        final String userId = requireNonNull(user.getId(), "user ID cannot be null");

        return sessionService.loadAll().stream()
                .filter(session -> userId.equals(session.getUserIdAttribute().orElse(null)))
                .map(MongoDbSession::getSessionId)
                .collect(Collectors.toSet());
    }
}
