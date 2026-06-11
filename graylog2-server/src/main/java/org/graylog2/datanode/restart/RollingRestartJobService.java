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
package org.graylog2.datanode.restart;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.InsertOneResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.datanode.restart.RollingRestartJob.FIELD_STATUS;
import static org.graylog2.datanode.restart.RollingRestartJob.FIELD_UPDATED_AT;

@Singleton
public class RollingRestartJobService {
    private static final Logger LOG = LoggerFactory.getLogger(RollingRestartJobService.class);
    static final String COLLECTION_NAME = "datanode_restart_jobs";

    private final MongoCollection<RollingRestartJob> collection;

    @Inject
    public RollingRestartJobService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, RollingRestartJob.class);
        try {
            collection.createIndex(
                    Indexes.ascending(FIELD_STATUS),
                    new IndexOptions()
                            .unique(true)
                            .partialFilterExpression(Filters.eq(FIELD_STATUS, RollingRestartJobStatus.ACTIVE.name()))
            );
        } catch (Exception e) {
            LOG.error("Failed to create datanode_restart_jobs index", e);
        }
    }

    public Optional<RollingRestartJob> findActive() {
        final Bson filter = Filters.in(FIELD_STATUS,
                RollingRestartJobStatus.ACTIVE.name(),
                RollingRestartJobStatus.PAUSED.name());
        return Optional.ofNullable(collection.find(filter).first());
    }

    public RollingRestartJob save(RollingRestartJob job) {
        final RollingRestartJob touched = job.withUpdatedAt(Instant.now());
        if (touched.id() == null) {
            final InsertOneResult result = collection.insertOne(touched);
            return touched.withId(insertedIdAsString(result));
        }
        collection.replaceOne(Filters.eq("_id", new ObjectId(touched.id())), touched);
        return touched;
    }

    public Optional<RollingRestartJob> findLastCompleted() {
        return Optional.ofNullable(
                collection.find(Filters.in(FIELD_STATUS,
                                RollingRestartJobStatus.COMPLETED.name(),
                                RollingRestartJobStatus.FAILED.name(),
                                RollingRestartJobStatus.ABORTED.name()))
                        .sort(Sorts.descending(FIELD_UPDATED_AT))
                        .first()
        );
    }

    public List<RollingRestartJob> findHistory(int limit) {
        final List<RollingRestartJob> out = new ArrayList<>();
        collection.find()
                .sort(Sorts.descending(FIELD_UPDATED_AT))
                .limit(limit)
                .into(out);
        return out;
    }
}
