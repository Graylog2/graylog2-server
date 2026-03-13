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
package org.graylog2.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterEventCleanupPeriodicalTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    private static final Instant TIME = Instant.parse("2015-04-01T00:00:00Z");
    private static final Duration maxEventAge = Duration.ofDays(1);
    private static final Clock FIXED_CLOCK = Clock.fixed(TIME, ZoneOffset.UTC);

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private MongoConnection mongoConnection;
    private ClusterEventCleanupPeriodical clusterEventCleanupPeriodical;

    @Before
    public void setUpService() {
        this.mongoConnection = mongodb.mongoConnection();

        this.clusterEventCleanupPeriodical = new ClusterEventCleanupPeriodical(new MongoCollections(
                new MongoJackObjectMapperProvider(objectMapper),
                mongodb.mongoConnection()), maxEventAge, FIXED_CLOCK);
    }

    @After
    public void tearDown() {
        mongoConnection.getMongoDatabase().drop();
    }

    @Test
    public void testDoRun() {
        final long maxEventAgeMillis = maxEventAge.toMillis();
        final long timeMillis = TIME.toEpochMilli();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        assertThat(insertEvent(collection, 0L)).isTrue();
        assertThat(insertEvent(collection, timeMillis)).isTrue();
        assertThat(insertEvent(collection, timeMillis - maxEventAgeMillis)).isTrue();
        assertThat(insertEvent(collection, timeMillis - 2 * maxEventAgeMillis)).isTrue();
        assertThat(collection.count()).isEqualTo(4L);

        clusterEventCleanupPeriodical.run();

        assertThat(collection.count()).isEqualTo(2L);
    }

    @Test
    public void getPeriodSeconds_defaultAge_returns43200() {
        final var mongoCollections = new MongoCollections(new MongoJackObjectMapperProvider(objectMapper), mongodb.mongoConnection());
        final var periodical = new ClusterEventCleanupPeriodical(mongoCollections, Duration.ofHours(12));
        assertThat(periodical.getPeriodSeconds()).isEqualTo(43200);
    }

    @Test
    public void getPeriodSeconds_shortAge_clampsToMinimum() {
        final var mongoCollections = new MongoCollections(new MongoJackObjectMapperProvider(objectMapper), mongodb.mongoConnection());
        final var periodical = new ClusterEventCleanupPeriodical(mongoCollections, Duration.ofSeconds(10));
        assertThat(periodical.getPeriodSeconds()).isEqualTo(3600);
    }

    @Test
    public void getPeriodSeconds_customAge_matchesMaxEventAge() {
        final var mongoCollections = new MongoCollections(new MongoJackObjectMapperProvider(objectMapper), mongodb.mongoConnection());
        final var periodical = new ClusterEventCleanupPeriodical(mongoCollections, Duration.ofHours(2));
        assertThat(periodical.getPeriodSeconds()).isEqualTo(7200);
    }

    private boolean insertEvent(DBCollection collection, long timestamp) {
        DBObject event = new BasicDBObjectBuilder()
                .add("timestamp", timestamp)
                .add("producer", "TEST-PRODUCER")
                .add("consumers", Collections.emptyList())
                .add("event_class", String.class.getCanonicalName())
                .add("payload", "Test" + timestamp)
                .get();
        return collection.save(event).wasAcknowledged();
    }
}
