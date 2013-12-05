/*
 * Copyright 2013 TORCH GmbH
 *
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
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.Validator;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MongoDbSession extends Persisted {
    private static final Logger log = LoggerFactory.getLogger(MongoDbSession.class);

    protected MongoDbSession(Core core, Map<String, Object> fields) {
        super(core, fields);
    }

    public MongoDbSession(ObjectId objectId, Map map, Core core) {
        super(core, objectId, map);
    }

    public static MongoDbSession load(String sessionId, Core core) {
        DBObject query = new BasicDBObject();
        query.put("session_id", sessionId);

        DBObject result = findOne(query, core, "sessions");
        if (result == null) {
            return null;
        }
        final Object objectId = result.get("_id");
        return new MongoDbSession((ObjectId) objectId, result.toMap(), core);
    }

    public static Collection<MongoDbSession> loadAll(Core core) {
        DBObject query = new BasicDBObject();
        List<MongoDbSession> dbSessions = Lists.newArrayList();
        final List<DBObject> sessions = query(query, core, "sessions");
        for (DBObject session : sessions) {
            dbSessions.add(new MongoDbSession((ObjectId) session.get("_id"), session.toMap(), core));
        }

        return dbSessions;
    }

    @Override
    public String getCollectionName() {
        return "sessions";
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return null;
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return null;
    }


    public Map<Object, Object> getAttributes() {
        final Object attributes = fields.get("attributes");
        if (attributes == null) {
            return null;
        }
        final ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) attributes);
        try {
            final ObjectInputStream ois = new ObjectInputStream(bis);
            final Object o = ois.readObject();
            return (Map<Object, Object>) o;
        } catch (IOException e) {
            log.error("little io. wow.", e);
        } catch (ClassNotFoundException e) {
            log.error("wrong thingy in db", e);
        }
        return null;
    }

    public void setAttributes(Map<Object, Object> attributes) {

        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(attributes);
            oos.close();
            fields.put("attributes", bos.toByteArray());
        } catch (IOException e) {
            log.error("too bad :(", e);
        }
    }


    public String getHost() {
        return (String) fields.get("host");
    }

    public long getTimeout() {
        final Object timeout = fields.get("timeout");
        if (timeout == null) return 0;
        return (Long) timeout;
    }

    public Date getStartTimestamp() {
        return ((DateTime) fields.get("start_timestamp")).toDate();
    }

    public Date getLastAccessTime() {
        return ((DateTime) fields.get("last_access_time")).toDate();
    }

    public void setHost(String host) {
        fields.put("host", host);
    }

    public void setTimeout(long timeout) {
        fields.put("timeout", timeout);
    }

    public void setStartTimestamp(Date startTimestamp) {
        fields.put("start_timestamp", startTimestamp);
    }

    public void setLastAccessTime(Date lastAccessTime) {
        fields.put("last_access_time", lastAccessTime);
    }

    public boolean isExpired() {
        final Object o = fields.get("expired");
        return o == null ? false : (Boolean) o;
    }

    public void setExpired(boolean expired) {
        fields.put("expired", expired);
    }

    public String getSessionId() {
        return String.valueOf(fields.get("session_id"));
    }
}
