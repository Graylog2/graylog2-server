/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.database;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import static java.util.Objects.requireNonNull;

public class MongoConnectionForTests implements MongoConnection {
    private final Mongo mongoClient;
    private final DB db;
    private final MongoDatabase mongoDatabase;

    public MongoConnectionForTests(Mongo mongoClient, String dbName) {
        this.mongoClient = requireNonNull(mongoClient);
        this.db = mongoClient.getDB(dbName);
        this.mongoDatabase = null;
    }

    public MongoConnectionForTests(MongoClient mongoClient, String dbName) {
        this.mongoClient = requireNonNull(mongoClient);
        this.db = mongoClient.getDB(dbName);
        this.mongoDatabase = mongoClient.getDatabase(dbName);
    }

    @Override
    public Mongo connect() {
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

    @Override
    public MongoClient getMongoClient() {
        return (MongoClient) mongoClient;
    }
}
