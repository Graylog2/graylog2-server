package org.graylog2.database;

import com.mongodb.client.MongoCollection;

public interface GraylogMongoCollection<TDocument> extends MongoCollection<TDocument> {
    TDocument save(TDocument dto);
}
