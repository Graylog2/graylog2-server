package org.graylog2.database;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

public interface MongoConnection {
    Mongo connect();

    DB getDatabase();
}
