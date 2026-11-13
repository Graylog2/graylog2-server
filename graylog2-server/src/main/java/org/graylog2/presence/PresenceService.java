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
package org.graylog2.presence;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

/**
 * Tracks which users are currently viewing a resource. Clients {@link #touch} a resource while they are viewing it,
 * usually from a request they poll anyway; a user counts as viewer as long as one of their clients touched it within
 * {@link #ACTIVE_WINDOW}. Entries live in MongoDB, so all nodes of a cluster see the same viewers.
 */
@Singleton
public class PresenceService {
    public static final String COLLECTION_NAME = "presence";
    public static final Duration ACTIVE_WINDOW = Duration.ofSeconds(10);

    private static final Duration WRITE_INTERVAL = Duration.ofSeconds(2);

    private record Key(String resource, String userId, String clientId) {}

    private final MongoCollection<PresenceEntry> collection;
    private final UserService userService;
    private final Clock clock;
    private final Cache<Key, Instant> lastWrites = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    @Inject
    public PresenceService(MongoCollections mongoCollections, UserService userService, Clock clock) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, PresenceEntry.class);
        this.userService = userService;
        this.clock = clock;

        collection.createIndex(Indexes.ascending(PresenceEntry.FIELD_RESOURCE, PresenceEntry.FIELD_USER_ID, PresenceEntry.FIELD_CLIENT_ID),
                new IndexOptions().unique(true));
        // Only garbage collection: MongoDB removes expired documents about once a minute, reads filter by ACTIVE_WINDOW.
        collection.createIndex(Indexes.ascending(PresenceEntry.FIELD_LAST_SEEN), new IndexOptions().expireAfter(1L, TimeUnit.MINUTES));
    }

    /**
     * Records that the given client of the user is viewing the resource. Writes at most once per
     * {@link #WRITE_INTERVAL} for the same client on this node.
     */
    public void touch(String resource, User user, String clientId) {
        requireNonBlank(resource, "resource cannot be blank");
        requireNonNull(user, "user cannot be null");
        requireNonBlank(clientId, "clientId cannot be blank");

        final var key = new Key(resource, user.getId(), clientId);
        final var now = clock.instant();
        final var lastWrite = lastWrites.getIfPresent(key);
        if (lastWrite != null && lastWrite.plus(WRITE_INTERVAL).isAfter(now)) {
            return;
        }

        try {
            collection.updateOne(Filters.and(
                            Filters.eq(PresenceEntry.FIELD_RESOURCE, resource),
                            Filters.eq(PresenceEntry.FIELD_USER_ID, key.userId()),
                            Filters.eq(PresenceEntry.FIELD_CLIENT_ID, clientId)),
                    Updates.set(PresenceEntry.FIELD_LAST_SEEN, Date.from(now)),
                    new UpdateOptions().upsert(true));
        } catch (MongoException e) {
            // A concurrent upsert of the same client already created the entry.
            if (!MongoUtils.isDuplicateKeyError(e)) {
                throw e;
            }
        }
        lastWrites.put(key, now);
    }

    /**
     * Returns the users currently viewing the resource, sorted by name.
     */
    public List<Viewer> viewers(String resource) {
        requireNonBlank(resource, "resource cannot be blank");
        final var since = Date.from(clock.instant().minus(ACTIVE_WINDOW));
        final var userIds = collection.distinct(PresenceEntry.FIELD_USER_ID, Filters.and(
                        Filters.eq(PresenceEntry.FIELD_RESOURCE, resource),
                        Filters.gt(PresenceEntry.FIELD_LAST_SEEN, since)), String.class)
                .into(new HashSet<>());
        if (userIds.isEmpty()) {
            return List.of();
        }

        return userService.loadByIds(userIds).stream()
                .map(user -> new Viewer(user.getId(), user.getName(), user.getFullName()))
                .sorted(Comparator.comparing(PresenceService::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Viewer::id))
                .toList();
    }

    public void removeResource(String resource) {
        requireNonBlank(resource, "resource cannot be blank");
        collection.deleteMany(Filters.eq(PresenceEntry.FIELD_RESOURCE, resource));
    }

    private static String displayName(Viewer viewer) {
        return viewer.fullName() == null || viewer.fullName().isBlank() ? viewer.username() : viewer.fullName();
    }
}
