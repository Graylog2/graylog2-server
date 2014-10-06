/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
        LOG.debug("Created session {}", id);
        final String objectId = mongoDBSessionService.saveWithoutValidation(dbSession);

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

        if(null == dbSession) {
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

        mongoDBSessionService.saveWithoutValidation(dbSession);
    }

    @Override
    protected void doDelete(Session session) {
        LOG.debug("Deleting session {}", session);
        final Serializable id = session.getId();
        final MongoDbSession dbSession = mongoDBSessionService.load(id.toString());
        mongoDBSessionService.destroy(dbSession);
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
