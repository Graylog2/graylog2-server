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
package org.graylog2.system.activities;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.indexer.IndexFailureImpl;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class SystemMessageServiceImpl extends PersistedServiceImpl implements SystemMessageService {
    private static final int MAX_COLLECTION_BYTES = 50 * 1024 * 1024;
    private final int PER_PAGE = 30;
    @Inject
    public SystemMessageServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);

        // Make sure that the system messages collection is always created capped.
        final String collectionName = SystemMessageImpl.class.getAnnotation(CollectionName.class).value();
        if (!mongoConnection.getDatabase().collectionExists(collectionName)) {
            final DBObject options = BasicDBObjectBuilder.start()
                    .add("capped", true)
                    .add("size", MAX_COLLECTION_BYTES)
                    .get();
            final DBCollection collection = mongoConnection.getDatabase().createCollection(collectionName, options);
            collection.createIndex(DBSort.desc("timestamp"));
        }
    }

    @Override
    public List<SystemMessage> all(int page) {
        List<SystemMessage> messages = Lists.newArrayList();

        DBObject sort = new BasicDBObject();
        sort.put("timestamp", -1);

        List<DBObject> results = query(SystemMessageImpl.class, new BasicDBObject(), sort, PER_PAGE, PER_PAGE * page);
        for (DBObject o : results) {
            messages.add(new SystemMessageImpl(new ObjectId(o.get("_id").toString()), o.toMap()));
        }

        return messages;
    }

    @Override
    public long totalCount() {
        return super.totalCount(SystemMessageImpl.class);
    }

    @Override
    public SystemMessage create(Map<String, Object> fields) {
        return new SystemMessageImpl(fields);
    }
}
