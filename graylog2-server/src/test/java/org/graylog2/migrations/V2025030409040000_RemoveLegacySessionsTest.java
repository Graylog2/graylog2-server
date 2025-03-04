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
package org.graylog2.migrations;

import com.mongodb.client.model.Filters;
import jakarta.annotation.Nonnull;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.security.SessionDeletedEvent;
import org.graylog2.security.sessions.MongoDbSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class V2025030409040000_RemoveLegacySessionsTest {

    private static class TestEventBus extends ClusterEventBus {
        private final List<Object> postedEvents = new ArrayList<>();

        @Override
        public void post(@Nonnull Object event) {
            postedEvents.add(event);
        }

        public List<Object> getPostedEvents() {
            return postedEvents;
        }
    }

    @Test
    void test(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider mapperProvider) {
        final var eventBus = new TestEventBus();
        final var mongoCollections = new MongoCollections(mapperProvider, mongoDBTestService.mongoConnection());

        final var collection = mongoCollections.nonEntityCollection(MongoDbSessionService.COLLECTION_NAME, Document.class);

        IntStream.range(0, 250).boxed().forEach(i -> {
            collection.insertOne(new Document(Map.of("session_id", "legacy-session-" + i,
                    "attributes", new byte[]{1, 2, 3})));
            collection.insertOne(new Document(Map.of("session_id", "session-" + i,
                    "attributes", Map.of("key", "value"))));
        });

        V2025030409040000_RemoveLegacySessions migration = new V2025030409040000_RemoveLegacySessions(
                mongoCollections, eventBus);

        migration.upgrade();

        assertThat(collection.countDocuments()).isEqualTo(250);
        assertThat(collection.countDocuments(Filters.regex("session_id", "legacy-session")))
                .isEqualTo(0);
        assertThat(eventBus.getPostedEvents()).hasSize(250).allMatch(event ->
                event instanceof SessionDeletedEvent sde && sde.sessionId().startsWith("legacy-session-"));
    }
}
