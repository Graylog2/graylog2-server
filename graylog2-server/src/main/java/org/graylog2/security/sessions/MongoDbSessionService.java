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
package org.graylog2.security.sessions;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.security.SessionDeletedEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog2.security.sessions.SessionDTO.FIELD_SESSION_ID;

@Singleton
public class MongoDbSessionService implements SessionService {
    public static final String COLLECTION_NAME = "sessions";
    private final ClusterEventBus eventBus;
    private final MongoCollection<SessionDTO> collection;

    @Inject
    public MongoDbSessionService(MongoCollections mongoCollections, ClusterEventBus clusterEventBus) {
        this.eventBus = clusterEventBus;
        this.collection = mongoCollections.collection(COLLECTION_NAME, SessionDTO.class);

        // Legacy: we had a non-unique index on session_id before. Remove it so that we can create the index again as
        // unique.
        final var indexes = collection.listIndexes().into(new ArrayList<>());
        indexes.stream()
                .filter(index -> index.getString("name").equals("session_id_1"))
                .filter(index -> !index.getBoolean("unique", false))
                .findFirst()
                .ifPresent(index -> collection.dropIndex("session_id_1"));

        collection.createIndex(Indexes.ascending(SessionDTO.FIELD_SESSION_ID), new IndexOptions().unique(true));
    }

    @Override
    @Nullable
    public Optional<SessionDTO> getBySessionId(String sessionId) {
        return Optional.ofNullable(collection.find(eq(FIELD_SESSION_ID, sessionId)).first());
    }

    @Override
    public boolean deleteBySessionId(String sessionId) {
        final var deleted = collection.deleteOne(eq(FIELD_SESSION_ID, sessionId)).getDeletedCount() > 0;
        if (deleted) {
            eventBus.post(new SessionDeletedEvent(sessionId));
            return true;
        }
        return false;
    }

    @Override
    public String create(SessionDTO session) {
        return MongoUtils.insertedIdAsString(collection.insertOne(session));
    }

    @Override
    public void update(SessionDTO session) {
        collection.replaceOne(MongoUtils.idEq(Objects.requireNonNull(session.id())), session);
    }

    @Override
    public Stream<SessionDTO> streamAll() {
        return MongoUtils.stream(collection.find());
    }
}
