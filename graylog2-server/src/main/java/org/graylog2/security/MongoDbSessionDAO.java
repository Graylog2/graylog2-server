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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MongoDbSessionDAO extends CachingSessionDAO {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbSessionDAO.class);

    private final MongoDBSessionService mongoDBSessionService;

    @Inject
    public MongoDbSessionDAO(MongoDBSessionService mongoDBSessionService, EventBus eventBus) {
        this.mongoDBSessionService = mongoDBSessionService;
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
        final Serializable id = generateSessionId(session);
        assignSessionId(session, id);

        Map<String, Object> fields = Maps.newHashMap();
        fields.put("session_id", id);
        fields.put("host", session.getHost());
        fields.put("start_timestamp", session.getStartTimestamp());
        fields.put("last_access_time", session.getLastAccessTime());
        fields.put("timeout", session.getTimeout());
        Map<Object, Object> attributes = Maps.newHashMap();
        for (Object key : session.getAttributeKeys()) {
            attributes.put(key.toString(), session.getAttribute(key));
        }
        final MongoDbSession dbSession = new MongoDbSession(fields);
        dbSession.setAttributes(attributes);
        final String objectId = mongoDBSessionService.saveWithoutValidation(dbSession);
        LOG.debug("Created session {}", objectId);

        return id;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        final MongoDbSession dbSession = mongoDBSessionService.load(sessionId.toString());
        if (dbSession == null) {
            // expired session or it was never there to begin with
            return null;
        }
        return mongoDBSessionService.daoToSimpleSession(dbSession);
    }

    @Override
    protected void doUpdate(Session session) {
        if (!(session instanceof SimpleSession simpleSession)) {
            throw new RuntimeException("Unsupported session type: " + session.getClass().getCanonicalName());
        }

        LOG.debug("Updating session");

        final String sessionId = session.getId().toString();
        final Map<String, Object> fields = Maps.newHashMap();
        fields.put("session_id", sessionId);
        fields.put("host", session.getHost());
        fields.put("start_timestamp", session.getStartTimestamp());
        fields.put("last_access_time", session.getLastAccessTime());
        fields.put("timeout", session.getTimeout());
        fields.put("expired", simpleSession.isExpired());

        final MongoDbSession dbSession = new MongoDbSession(fields);
        dbSession.setAttributes(simpleSession.getAttributes());

        mongoDBSessionService.updateBySessionId(sessionId, dbSession);
    }

    @Override
    protected void doDelete(Session session) {
        LOG.debug("Deleting session");
        final Serializable id = session.getId();
        final MongoDbSession dbSession = mongoDBSessionService.load(id.toString());
        if (dbSession != null) {
            final int deleted = mongoDBSessionService.destroy(dbSession);
            LOG.debug("Deleted {} sessions from database", deleted);
        } else {
            LOG.debug("Session not found in database");
        }
    }

    @Override
    public Collection<Session> getActiveSessions() {
        LOG.debug("Retrieving all active sessions.");

        Collection<MongoDbSession> dbSessions = mongoDBSessionService.loadAll();
        List<Session> sessions = Lists.newArrayList();
        for (MongoDbSession dbSession : dbSessions) {
            sessions.add(mongoDBSessionService.daoToSimpleSession(dbSession));
        }

        return sessions;
    }
}
