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
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class MongoDBSessionServiceImpl extends PersistedServiceImpl implements MongoDBSessionService {
    @Inject
    public MongoDBSessionServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);
    }

    @Override
    public MongoDbSession load(String sessionId) {
        DBObject query = new BasicDBObject();
        query.put("session_id", sessionId);

        DBObject result = findOne(MongoDbSession.class, query);
        if (result == null) {
            return null;
        }
        final Object objectId = result.get("_id");
        return new MongoDbSession((ObjectId) objectId, result.toMap());
    }

    @Override
    public Collection<MongoDbSession> loadAll() {
        DBObject query = new BasicDBObject();
        List<MongoDbSession> dbSessions = Lists.newArrayList();
        final List<DBObject> sessions = query(MongoDbSession.class, query);
        for (DBObject session : sessions) {
            dbSessions.add(new MongoDbSession((ObjectId) session.get("_id"), session.toMap()));
        }

        return dbSessions;
    }
}