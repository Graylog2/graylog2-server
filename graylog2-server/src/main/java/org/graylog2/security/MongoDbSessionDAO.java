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

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.DuplicateKeyException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MongoDbSessionDAO extends CachingSessionDAO {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbSessionDAO.class);

    private final MongoDBSessionService mongoDBSessionService;

    @Inject
    public MongoDbSessionDAO(MongoDBSessionService mongoDBSessionService) {
        this.mongoDBSessionService = mongoDBSessionService;
    }

    @Override
    protected Serializable doCreate(Session session) {
        final Serializable id = generateSessionId(session);
        assignSessionId(session, id);

        Map<String, Object> fields = Maps.newHashMap();
        fields.put("session_id", id);
        fields.put("host", session.getHost());
        fields.put("start_timestamp", session.getStartTimestamp());
        fields.put("last_access_time", session.getLastAccessTime());
        fields.put("timeout", session.getTimeout());
        Map<String, Object> attributes = Maps.newHashMap();
        for (Object key : session.getAttributeKeys()) {
            attributes.put(key.toString(), session.getAttribute(key));
        }
        fields.put("attributes", attributes);
        final MongoDbSession dbSession = new MongoDbSession(fields);
        final String objectId = mongoDBSessionService.saveWithoutValidation(dbSession);
        LOG.debug("Created session {}", objectId);

        return id;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        final MongoDbSession dbSession = mongoDBSessionService.load(sessionId.toString());
        LOG.debug("Reading session for id {} from MongoDB: {}", sessionId, dbSession);
        if (dbSession == null) {
            // expired session or it was never there to begin with
            return null;
        }
        return getSimpleSession(sessionId, dbSession);
    }

    private SimpleSession getSimpleSession(Serializable sessionId, MongoDbSession dbSession) {
        final SimpleSession session = new SimpleSession();
        assignSessionId(session, sessionId);
        session.setHost(dbSession.getHost());
        session.setTimeout(dbSession.getTimeout());
        session.setStartTimestamp(dbSession.getStartTimestamp());
        session.setLastAccessTime(dbSession.getLastAccessTime());
        session.setExpired(dbSession.isExpired());
        session.setAttributes(dbSession.getAttributes());
        return session;
    }

    @Override
    protected void doUpdate(Session session) {
        final MongoDbSession dbSession = mongoDBSessionService.load(session.getId().toString());

        if (null == dbSession) {
            throw new RuntimeException("Couldn't load session <" + session.getId() + ">");
        }

        LOG.debug("Updating session {}", session);
        dbSession.setHost(session.getHost());
        dbSession.setTimeout(session.getTimeout());
        dbSession.setStartTimestamp(session.getStartTimestamp());
        dbSession.setLastAccessTime(session.getLastAccessTime());

        if (session instanceof SimpleSession) {
            final SimpleSession simpleSession = (SimpleSession) session;
            dbSession.setAttributes(simpleSession.getAttributes());
            dbSession.setExpired(simpleSession.isExpired());
        } else {
            throw new RuntimeException("Unsupported session type: " + session.getClass().getCanonicalName());
        }
        // Due to https://jira.mongodb.org/browse/SERVER-14322 upserts can fail under concurrency.
        // We need to retry the update, and stagger them a bit, so no all of the retries attempt it at the same time again.
        // Usually this should succeed the first time, though
        final Retryer<Object> retryer = RetryerBuilder.newBuilder()
                .retryIfExceptionOfType(DuplicateKeyException.class)
                .withWaitStrategy(WaitStrategies.randomWait(5, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(10))
                .build();
        try {
            retryer.call(() -> mongoDBSessionService.saveWithoutValidation(dbSession));
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
        LOG.debug("Deleting session {}", session);
        final Serializable id = session.getId();
        final MongoDbSession dbSession = mongoDBSessionService.load(id.toString());
        if (dbSession != null) {
            final int deleted = mongoDBSessionService.destroy(dbSession);
            LOG.debug("Deleted {} sessions with ID {} from database", deleted, id);
        } else {
            LOG.debug("Session {} not found in database", id);
        }
    }

    @Override
    public Collection<Session> getActiveSessions() {
        LOG.debug("Retrieving all active sessions.");

        Collection<MongoDbSession> dbSessions = mongoDBSessionService.loadAll();
        List<Session> sessions = Lists.newArrayList();
        for (MongoDbSession dbSession : dbSessions) {
            sessions.add(getSimpleSession(dbSession.getSessionId(), dbSession));
        }

        return sessions;
    }
}
