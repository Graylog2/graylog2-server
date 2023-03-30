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
package org.graylog2.database;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import static java.util.Objects.requireNonNull;

public class MongoConnectionForTests implements MongoConnection {
    private final MongoClient mongoClient;
    private final DB db;
    private final MongoDatabase mongoDatabase;

    public MongoConnectionForTests(MongoClient mongoClient, String dbName) {
        this.mongoClient = requireNonNull(mongoClient);
        this.db = mongoClient.getDB(dbName);
        this.mongoDatabase = mongoClient.getDatabase(dbName);
    }

    @Override
    public MongoClient connect() {
        return mongoClient;
    }

    @Override
    public DB getDatabase() {
        return db;
    }

    @Override
    public MongoDatabase getMongoDatabase() {
        if(mongoDatabase == null) {
            throw new IllegalStateException("MongoDatabase is unavailable.");
        }

        return mongoDatabase;
    }
}
