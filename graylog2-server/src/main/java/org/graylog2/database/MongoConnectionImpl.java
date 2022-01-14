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
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;
import org.graylog2.configuration.MongoDbConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * MongoDB connection singleton
 */
@Singleton
public class MongoConnectionImpl implements MongoConnection {
    private static final Logger LOG = LoggerFactory.getLogger(MongoConnectionImpl.class);

    private final MongoClientURI mongoClientURI;

    private MongoClient m = null;
    private DB db = null;
    private MongoDatabase mongoDatabase = null;

    @Inject
    public MongoConnectionImpl(final MongoDbConfiguration configuration) {
        this(configuration.getMongoClientURI());
    }

    MongoConnectionImpl(MongoClientURI mongoClientURI) {
        this.mongoClientURI = checkNotNull(mongoClientURI);
    }

    /**
     * Connect the instance.
     */
    @Override
    public synchronized Mongo connect() {
        if (m == null) {
            final String dbName = mongoClientURI.getDatabase();
            if (isNullOrEmpty(dbName)) {
                LOG.error("The MongoDB database name must not be null or empty (mongodb_uri was: {})", mongoClientURI);
                throw new RuntimeException("MongoDB database name is missing.");
            }

            m = new MongoClient(mongoClientURI);
            db = m.getDB(dbName);
            db.setWriteConcern(WriteConcern.ACKNOWLEDGED);

            mongoDatabase = m.getDatabase(dbName).withWriteConcern(WriteConcern.ACKNOWLEDGED);
        }

        try {
            db.command("{ ping: 1 }");
        } catch (MongoCommandException e) {
            if (e.getCode() == 18) {
                throw new MongoException("Couldn't connect to MongoDB. Please check the authentication credentials.", e);
            } else {
                throw new MongoException("Couldn't connect to MongoDB: " + e.getMessage(), e);
            }
        }
        return m;
    }

    /**
     * Returns the raw database object.
     *
     * @return database
     */
    @Override
    public DB getDatabase() {
        return db;
    }

    @Override
    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }
}
