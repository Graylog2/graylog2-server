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
package org.graylog2.cluster.lock;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.NodeId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.currentDate;
import static com.mongodb.client.model.Updates.setOnInsert;
import static org.graylog2.cluster.lock.Lock.FIELD_LOCKED_BY;
import static org.graylog2.cluster.lock.Lock.FIELD_RESOURCE;
import static org.graylog2.cluster.lock.Lock.FIELD_UPDATED_AT;
import static org.graylog2.database.indices.MongoDbIndexTools.ensureTTLIndex;

/**
 * Lock service implementation using MongoDB to maintain locks.
 * Unless a lock is kept alive by periodically re-requesting the same lock, it will eventually expire.
 * Lock expiry is handled by MongoDB internally. We set lock expiry to 60 seconds, but in practice it may take up to ~2
 * minutes until a lock really expires.
 */
@Singleton
public class MongoLockService implements LockService {

    public static final String COLLECTION_NAME = "cluster_locks";
    public static final java.time.Duration MIN_LOCK_TTL = Duration.ofSeconds(60);

    private final NodeId nodeId;
    private final MongoCollection<Document> collection;

    @Inject
    public MongoLockService(NodeId nodeId,
                            MongoConnection mongoConnection,
                            @Named("lock_service_lock_ttl") Duration lockTTL) {
        this.nodeId = nodeId;

        collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        collection.createIndex(Indexes.ascending(FIELD_RESOURCE), new IndexOptions().unique(true));

        ensureTTLIndex(collection, lockTTL, FIELD_UPDATED_AT);
    }

    @Override
    public Optional<Lock> lock(@Nonnull String resource, @Nullable String lockContext) {
        return doLock(resource, getLockedByString(lockContext));
    }
    @Override
    public Optional<Lock> lock(@Nonnull String resource) {
        return lock(resource, null);
    }

    @Override
    public Optional<Lock> extendLock(@Nonnull Lock lock) {
        if (lock != null) {
            return doLock(lock.resource(), lock.lockedBy());
        }
        return Optional.empty();
    }

    private Optional<Lock> doLock(@Nonnull String resource, @Nonnull String lockedBy) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(resource));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(lockedBy));

        try {
            final Document doc = collection.findOneAndUpdate(
                    and(eq(FIELD_RESOURCE, resource), eq(FIELD_LOCKED_BY, lockedBy)),
                    Updates.combine(
                            currentDate(FIELD_UPDATED_AT),
                            setOnInsert(FIELD_RESOURCE, resource),
                            setOnInsert(FIELD_LOCKED_BY, lockedBy)
                    ),
                    new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));

            return Optional.of(toLock(Objects.requireNonNull(doc)));
        } catch (MongoCommandException e) {
            // Getting a duplicate key exception here means that there is a lock already, but we are not the owner.
            if (e.getCode() == 11000) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public Optional<Lock> unlock(@Nonnull String resource, @Nullable String lockContext) {
        return doUnlock(resource, getLockedByString(lockContext));
    }

    @Override
    public Optional<Lock> unlock(@Nonnull Lock lock) {
        if (lock != null) {
            return doUnlock(lock.resource(), lock.lockedBy());
        }
        return Optional.empty();
    }

    private Optional<Lock> doUnlock(@Nonnull String resource, @Nonnull String lockedBy) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(resource));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(lockedBy));

        final Document deletedDocument =
                collection.findOneAndDelete(
                        and(eq(FIELD_RESOURCE, resource), eq(FIELD_LOCKED_BY, lockedBy)));

        if (deletedDocument != null) {
            return Optional.of(toLock(deletedDocument));
        }

        return Optional.empty();
    }

    private Lock toLock(Document doc) {
        final ZonedDateTime createdAt =
                Instant.ofEpochSecond(doc.getObjectId("_id").getTimestamp())
                        .atZone(ZoneOffset.UTC);

        final ZonedDateTime updatedAt = doc.getDate(FIELD_UPDATED_AT).toInstant()
                .atZone(ZoneOffset.UTC);

        return Lock.builder()
                .resource(doc.getString(FIELD_RESOURCE))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .lockedBy(doc.getString(FIELD_LOCKED_BY))
                .build();
    }

    private String getLockedByString(@Nullable String lockContext) {
        if (lockContext == null) {
            return nodeId.getNodeId();
        }
        return nodeId.getNodeId() + "-" + lockContext;
    }
}

