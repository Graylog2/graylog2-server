package org.graylog2.database;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public interface MongoConnection {
    MongoClient connect();

    DB getDatabase();
}
