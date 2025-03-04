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

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mongodb.MongoException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.security.SessionDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

        final var primaryKey = sessionService.create(SessionDTO.fromSimpleSession(session));
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
        final var databaseId = sessionService.getBySessionId(session.getId().toString()).map(SessionDTO::id)
                .orElseThrow(() -> new RuntimeException("Couldn't load session"));

        LOG.debug("Updating session");

        final var sessionDTO = SessionDTO.fromSimpleSession(session, databaseId);

        // Due to https://jira.mongodb.org/browse/SERVER-14322 upserts can fail under concurrency.
        // We need to retry the update, and stagger them a bit, so no all of the retries attempt it at the same time again.
        // Usually this should succeed the first time, though
        final var retryer = RetryerBuilder.<Void>newBuilder()
                .retryIfException(e -> e instanceof MongoException me && MongoUtils.isDuplicateKeyError(me))
                .withWaitStrategy(WaitStrategies.randomWait(5, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(10))
                .build();
        try {
            retryer.call(() -> {
                sessionService.update(sessionDTO);
                return null;
            });
        } catch (ExecutionException e) {
            LOG.warn("Unexpected exception when saving session to MongoDB. Failed to update session.", e);
            throw new RuntimeException(e.getCause());
        } catch (RetryException e) {
            LOG.warn("Tried to update session 10 times, but still failed. This is likely because of https://jira.mongodb.org/browse/SERVER-14322", e);
            throw new RuntimeException(e.getCause());
        }
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

        return sessionService.streamAll().<Session>map(SessionDTO::toSimpleSession).toList();
    }
}
