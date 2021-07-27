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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.events.UserChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * This service checks on startup if all user sessions should be terminated. This can be requested by bumping the
 * {@code TERMINATION_REVISION} number.
 *
 * The service also listens for {@link UserChangedEvent} events and terminates all sessions for disabled users.
 */
@Singleton
public class UserSessionTerminationService extends AbstractIdleService {
    /**
     * The revision must be bumped if all active user sessions should be terminated on server
     * startup after a Graylog update.
     */
    private static final long TERMINATION_REVISION = 1;

    private static final Logger LOG = LoggerFactory.getLogger(UserSessionTerminationService.class);

    // Sessions should be terminated when a user's account status is one of the following
    private static final EnumSet<User.AccountStatus> SESSION_TERMINATION_STATUS = EnumSet.of(
            User.AccountStatus.DELETED,
            User.AccountStatus.DISABLED
    );

    private final MongoDbSessionDAO sessionDao;
    private final MongoDBSessionService sessionService;
    private final DefaultSecurityManager securityManager;
    private final ClusterConfigService clusterConfigService;
    private final UserService userService;
    private final ServerStatus serverStatus;
    private final EventBus eventBus;

    @Inject
    public UserSessionTerminationService(MongoDbSessionDAO sessionDao,
                                         MongoDBSessionService sessionService,
                                         DefaultSecurityManager securityManager,
                                         ClusterConfigService clusterConfigService,
                                         UserService userService,
                                         ServerStatus serverStatus,
                                         EventBus eventBus) {
        this.sessionDao = sessionDao;
        this.securityManager = securityManager;
        this.clusterConfigService = clusterConfigService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.serverStatus = serverStatus;
        this.eventBus = eventBus;
    }

    // Listens on the event bus to terminate users sessions when a user gets disabled or deleted.
    @Subscribe
    public void handleUserChanged(UserChangedEvent event) {
        if (isNotPrimaryNode()) {
            LOG.debug("Only run on the primary node to avoid concurrent session termination");
            return;
        }

        final User user = userService.loadById(event.userId());

        if (user != null && SESSION_TERMINATION_STATUS.contains(user.getAccountStatus())) {
            terminateSessionsForUser(user);
        }
    }

    @Override
    protected void startUp() throws Exception {
        try {
            runGlobalSessionTermination();
        } catch (Exception e) {
            LOG.error("Couldn't run global session termination", e);
        }

        eventBus.register(this);
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
    }

    private boolean isNotPrimaryNode() {
        return !serverStatus.hasCapability(ServerStatus.Capability.MASTER);
    }

    // Terminates all active user sessions when the TERMINATION_REVISION has been bumped.
    private void runGlobalSessionTermination() {
        if (isNotPrimaryNode()) {
            LOG.debug("Only run on the primary node to avoid concurrent session termination");
            return;
        }

        final GlobalTerminationRevisionConfig globalTerminationRevisionConfig = clusterConfigService.getOrDefault(
                GlobalTerminationRevisionConfig.class,
                GlobalTerminationRevisionConfig.initial()
        );

        if (!globalTerminationRevisionConfig.isOutdated()) {
            LOG.debug("Global session termination not required");
            return;
        }

        final AtomicLong terminatedSessions = new AtomicLong(0);
        for (final Session activeSession : sessionDao.getActiveSessions()) {
            terminateSessionForID(activeSession.getId());
            terminatedSessions.incrementAndGet();
        }

        LOG.info("Globally terminated {} session(s)", terminatedSessions.get());
        clusterConfigService.write(GlobalTerminationRevisionConfig.next());
    }

    private void terminateSessionsForUser(User user) {
        try {
            final Set<String> sessionIds = getSessionIDsForUser(user);

            for (final String sessionId : sessionIds) {
                getSessionForID(sessionId).ifPresent(session -> {
                    LOG.info("Terminating session for user <{}/{}>", user.getName(), user.getId());
                    session.stop();
                });
            }
        } catch (Exception e) {
            LOG.error("Couldn't terminate session for user <{}/{}>", user.getName(), user.getId(), e);
        }
    }

    private void terminateSessionForID(Serializable sessionId) {
        try {
            getSessionForID(sessionId).ifPresent(Session::stop);
        } catch (Exception e) {
            LOG.error("Couldn't terminate session", e);
        }
    }

    private Optional<Session> getSessionForID(Serializable sessionId) {
        final SessionManager sessionManager = securityManager.getSessionManager();

        // Using the session manager to get the session instead of getting it directly from the SessionDAO
        // because the session manager wraps it in a DelegatingSession that might do additional cleanup.
        return Optional.ofNullable(sessionManager.getSession(new DefaultSessionKey(sessionId)));
    }

    private Set<String> getSessionIDsForUser(User user) {
        final String userId = requireNonNull(user.getId(), "user ID cannot be null");

        return sessionService.loadAll().stream()
                .filter(session -> userId.equals(session.getUserIdAttribute().orElse(null)))
                .map(MongoDbSession::getSessionId)
                .collect(Collectors.toSet());
    }

    @AutoValue
    public static abstract class GlobalTerminationRevisionConfig {
        @JsonProperty("applied_revision")
        public abstract long appliedRevision();

        public static GlobalTerminationRevisionConfig initial() {
            // Default to 0 to make sure sessions get cleaned up on first run
            return create(0);
        }

        public static GlobalTerminationRevisionConfig next() {
            return create(TERMINATION_REVISION);
        }

        @JsonIgnore
        public boolean isOutdated() {
            return appliedRevision() < TERMINATION_REVISION;
        }

        @JsonCreator
        public static GlobalTerminationRevisionConfig create(@JsonProperty("applied_revision") long appliedRevision) {
            return new AutoValue_UserSessionTerminationService_GlobalTerminationRevisionConfig(appliedRevision);
        }
    }
}
