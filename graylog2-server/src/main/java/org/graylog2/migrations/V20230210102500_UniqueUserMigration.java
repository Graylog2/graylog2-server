package org.graylog2.migrations;

import com.mongodb.DBCollection;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.users.UserImpl;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20230210102500_UniqueUserMigration extends Migration {
    private final IndexSetService indexSetService;
    private final MongoConnection mongoConnection;

    @Inject
    public V20230210102500_UniqueUserMigration(IndexSetService indexSetService, MongoConnection mongoConnection) {
        this.indexSetService = indexSetService;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-02-10T10:25:00Z");
    }

    @Override
    public void upgrade() {
        DBCollection coll = mongoConnection.getDatabase().getCollection(UserImpl.COLLECTION_NAME);
        coll.createIndex(DBSort.asc(UserImpl.USERNAME), "unique_username", true);
    }
}
