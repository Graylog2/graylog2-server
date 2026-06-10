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
package org.graylog2.security.sessions;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.graylog2.security.SessionDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;

/**
 * A Shiro SessionDAO that persists sessions to MongoDB. Although its methods accept and return sessions that
 * implement the generic {@link Session} interface, they are restricted to {@link SimpleSession} instances and
 * are only allowed to hold a single principal and a limited set of attributes.
 * <p>
 * For the list of allowed attributes see {@link SessionConverter#KNOWN_SESSION_KEYS}.
 */
@Singleton
public class SessionDAO extends CachingSessionDAO {
    private static final Logger LOG = LoggerFactory.getLogger(SessionDAO.class);

    private final SessionService sessionService;

    @Inject
    public SessionDAO(SessionService sessionService, EventBus eventBus) {
        this.sessionService = sessionService;
        eventBus.register(this);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void sessionDeleted(SessionDeletedEvent event) {
        final Session cachedSession = getCachedSession(event.sessionId());
        if (cachedSession != null) {
            LOG.debug("Removing deleted session from cache.");
            uncache(cachedSession);
        }
    }

    @Override
    protected Serializable doCreate(Session session) {
        if (!(session instanceof SimpleSession)) {
            throw new RuntimeException("Unsupported session type: " + session.getClass().getCanonicalName());
        }
        return doCreate((SimpleSession) session);
    }

    private String doCreate(SimpleSession session) {
        final String sessionId = generateSessionId(session).toString();

        assignSessionId(session, sessionId);

        final var primaryKey = sessionService.create(SessionDTO.builderFromSimpleSession(session).build());
        LOG.debug("Created session {}", primaryKey);

        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        return sessionService.getBySessionId(sessionId.toString()).map(SessionDTO::toSimpleSession).orElse(null);
    }

    @Override
    protected void doUpdate(Session session) {
        if (!(session instanceof SimpleSession)) {
            throw new RuntimeException("Unsupported session type: " + session.getClass().getCanonicalName());
        }
        doUpdate((SimpleSession) session);
    }

    private void doUpdate(SimpleSession session) {
        LOG.debug("Updating session");
        final var sessionDTO = SessionDTO.builderFromSimpleSession(session).build();
        sessionService.updateBySessionId(sessionDTO);
    }

    @Override
    protected void doDelete(Session session) {
        LOG.debug("Deleting session");
        if (sessionService.deleteBySessionId(session.getId().toString())) {
            LOG.debug("Deleted session from database");
        } else {
            LOG.debug("Session not found in database");
        }
    }

    @Override
    public Collection<Session> getActiveSessions() {
        LOG.debug("Retrieving all active sessions.");

        try (var sessionDTOStream = sessionService.streamAll()) {
            return sessionDTOStream.<Session>map(SessionDTO::toSimpleSession).toList();
        }
    }
}
