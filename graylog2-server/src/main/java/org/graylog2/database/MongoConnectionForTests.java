package org.graylog2.database;

import com.mongodb.DB;
import com.mongodb.Mongo;

public class MongoConnectionForTests implements MongoConnection {
    private final Mongo mongoClient;
    private final DB db;

    public MongoConnectionForTests(Mongo mongoClient, String dbName) {
        this.mongoClient = mongoClient;
        this.db = mongoClient.getDB(dbName);
    }

    @Override
    public Mongo connect() {
        return mongoClient;
    }

    @Override
    public DB getDatabase() {
        return db;
    }
}
