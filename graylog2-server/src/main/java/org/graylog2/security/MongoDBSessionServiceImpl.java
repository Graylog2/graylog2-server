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
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;

@Singleton
public class MongoDBSessionServiceImpl extends PersistedServiceImpl implements MongoDBSessionService {
    @Inject
    public MongoDBSessionServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);

        final MongoDatabase database = mongoConnection.getMongoDatabase();
        final MongoCollection<Document> sessions = database.getCollection(MongoDbSession.COLLECTION_NAME);
        sessions.createIndex(Indexes.ascending(MongoDbSession.FIELD_SESSION_ID));
    }

    @Override
    @Nullable
    public MongoDbSession load(String sessionId) {
        DBObject query = new BasicDBObject();
        query.put(MongoDbSession.FIELD_SESSION_ID, sessionId);

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