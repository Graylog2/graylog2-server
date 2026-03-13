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

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ClusterEventCleanupPeriodicalTest {
    private static final DateTime TIME = new DateTime(2015, 4, 1, 0, 0, DateTimeZone.UTC);
    private static final Duration maxEventAge = Duration.ofDays(1);

    private MongoConnection mongoConnection;
    private ClusterEventCleanupPeriodical clusterEventCleanupPeriodical;

    @BeforeEach
    public void setUpService(MongoCollections mongoCollections) throws Exception {
        DateTimeUtils.setCurrentMillisFixed(TIME.getMillis());

        this.mongoConnection = mongoCollections.mongoConnection();

        this.clusterEventCleanupPeriodical = new ClusterEventCleanupPeriodical(mongoCollections, maxEventAge);
    }

    @AfterEach
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
        mongoConnection.getMongoDatabase().drop();
    }

    @Test
    public void testDoRun() throws Exception {
        final var maxEventAgeMillis = maxEventAge.toMillis();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        assertThat(insertEvent(collection, 0L)).isTrue();
        assertThat(insertEvent(collection, TIME.getMillis())).isTrue();
        assertThat(insertEvent(collection, TIME.minus(maxEventAgeMillis).getMillis())).isTrue();
        assertThat(insertEvent(collection, TIME.minus(2 * maxEventAgeMillis).getMillis())).isTrue();
        assertThat(collection.count()).isEqualTo(4L);

        clusterEventCleanupPeriodical.run();

        assertThat(collection.count()).isEqualTo(2L);
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
