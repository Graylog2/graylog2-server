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
import com.mongodb.DBObject;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.types.ObjectId;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.PersistedServiceImpl;
import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Singleton
public class SystemMessageServiceImpl extends PersistedServiceImpl implements SystemMessageService {
    private static final int MAX_COLLECTION_BYTES = 50 * 1024 * 1024;
    private static final int PER_PAGE = 30;

    @Inject
    public SystemMessageServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);

        // Make sure that the system messages collection is always created capped.
        final String collectionName = SystemMessageImpl.class.getAnnotation(DbEntity.class).collection();
        final var database = mongoConnection.getMongoDatabase();
        if (!database.listCollectionNames().into(new HashSet<>()).contains(collectionName)) {
            final var options = new CreateCollectionOptions()
                    .capped(true)
                    .sizeInBytes(MAX_COLLECTION_BYTES);
            database.createCollection(collectionName, options);
            database.getCollection(collectionName).createIndex(Indexes.descending("timestamp"));
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
    public PaginatedList<SystemMessage> find(String query, @Nullable DateTime since, int page) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }
        final BasicDBObject filter = new BasicDBObject();
        final String text = query == null ? "" : query.strip();
        if (!text.isEmpty()) {
            final Pattern pattern = Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);
            filter.put("$or", List.of(new BasicDBObject("content", pattern), new BasicDBObject("caller", pattern)));
        }
        if (since != null) {
            filter.put("timestamp", new BasicDBObject("$gte", since.toDate()));
        }
        final List<SystemMessage> messages = query(SystemMessageImpl.class, filter, new BasicDBObject("timestamp", -1),
                PER_PAGE, PER_PAGE * (page - 1)).stream()
                .map(o -> (SystemMessage) new SystemMessageImpl(new ObjectId(o.get("_id").toString()), o.toMap()))
                .toList();
        return new PaginatedList<>(messages, Math.toIntExact(count(SystemMessageImpl.class, filter)), page, PER_PAGE);
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
