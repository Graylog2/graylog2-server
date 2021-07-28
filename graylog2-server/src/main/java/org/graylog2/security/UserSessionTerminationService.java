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
package org.graylog2.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.ExpiredSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.users.events.UserChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.Optional;

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

    private final MongoDbSessionDAO sessionDao;
    private final DefaultSecurityManager securityManager;
    private final ClusterConfigService clusterConfigService;
    private final ServerStatus serverStatus;

    @Inject
    public UserSessionTerminationService(MongoDbSessionDAO sessionDao,
                                         DefaultSecurityManager securityManager,
                                         ClusterConfigService clusterConfigService,
                                         ServerStatus serverStatus) {
        this.sessionDao = sessionDao;
        this.securityManager = securityManager;
        this.clusterConfigService = clusterConfigService;
        this.serverStatus = serverStatus;
    }

    @Override
    protected void startUp() throws Exception {
        try {
            runGlobalSessionTermination();
        } catch (Exception e) {
            LOG.error("Couldn't run global session termination", e);
        }
    }

    @Override
    protected void shutDown() throws Exception {
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

        long terminatedSessions = 0;
        for (final Session activeSession : sessionDao.getActiveSessions()) {
            terminateSessionForID(activeSession.getId());
            terminatedSessions++;
        }

        LOG.info("Globally terminated {} session(s)", terminatedSessions);
        clusterConfigService.write(GlobalTerminationRevisionConfig.withCurrentRevision());
    }

    private void terminateSessionForID(Serializable sessionId) {
        try {
            getActiveSessionForID(sessionId).ifPresent(Session::stop);
        } catch (Exception e) {
            LOG.error("Couldn't terminate session", e);
        }
    }

    private Optional<Session> getActiveSessionForID(Serializable sessionId) {
        final SessionManager sessionManager = securityManager.getSessionManager();

        // Using the session manager to get the session instead of getting it directly from the SessionDAO
        // because the session manager wraps it in a DelegatingSession that might do additional cleanup.
        try {
            return Optional.ofNullable(sessionManager.getSession(new DefaultSessionKey(sessionId)));
        } catch (ExpiredSessionException e) {
            return Optional.empty();
        }
    }

    @AutoValue
    public static abstract class GlobalTerminationRevisionConfig {
        @JsonProperty("applied_revision")
        public abstract long appliedRevision();

        public static GlobalTerminationRevisionConfig initial() {
            // Default to 0 to make sure sessions get cleaned up on first run
            return create(0);
        }

        public static GlobalTerminationRevisionConfig withCurrentRevision() {
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
