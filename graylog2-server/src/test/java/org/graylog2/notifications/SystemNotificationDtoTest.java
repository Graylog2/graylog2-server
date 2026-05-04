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
package org.graylog2.notifications;

import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class SystemNotificationDtoTest {

    private static final String COLLECTION_NAME = "system_notifications";

    private MongoCollections mongoCollections;
    private MongoCollection<SystemNotificationDto> collection;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.mongoCollections = mongoCollections;
        this.collection = mongoCollections.collection(COLLECTION_NAME, SystemNotificationDto.class);
    }

    @Test
    void roundTripWithAllFields() {
        final Instant triggeredAt = Instant.parse("2026-04-20T08:15:00Z");
        final Instant lastChanged = Instant.parse("2026-04-21T10:30:00Z");

        final SystemNotificationDto original = SystemNotificationDto.builder()
                .type("es_cluster_red")
                .key("some-key")
                .severity("urgent")
                .nodeId("node-1")
                .title("Indexer cluster unhealthy")
                .description("The cluster state is RED")
                .details(Map.of("reason", "shards unassigned"))
                .isRead(true)
                .actor(SystemNotificationDto.Actor.create("admin", "Administrator"))
                .lastChanged(lastChanged)
                .triggeredAt(triggeredAt)
                .build();

        collection.insertOne(original);
        final SystemNotificationDto result = collection.find().first();

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.type()).isEqualTo("es_cluster_red");
        assertThat(result.key()).isEqualTo("some-key");
        assertThat(result.severity()).isEqualTo("urgent");
        assertThat(result.nodeId()).isEqualTo("node-1");
        assertThat(result.title()).isEqualTo("Indexer cluster unhealthy");
        assertThat(result.description()).isEqualTo("The cluster state is RED");
        assertThat(result.details()).containsEntry("reason", "shards unassigned");
        assertThat(result.isRead()).isTrue();
        assertThat(result.actor()).isNotNull();
        assertThat(result.actor().id()).isEqualTo("admin");
        assertThat(result.actor().name()).isEqualTo("Administrator");
        assertThat(result.lastChanged()).isEqualTo(lastChanged);
        assertThat(result.triggeredAt()).isEqualTo(triggeredAt);
    }

    @Test
    void roundTripWithNullableFields() {
        final Instant triggeredAt = Instant.parse("2026-04-20T08:15:00Z");

        final SystemNotificationDto original = SystemNotificationDto.builder()
                .type("es_unavailable")
                .severity("normal")
                .nodeId("node-1")
                .details(Map.of())
                .isRead(false)
                .triggeredAt(triggeredAt)
                .build();

        collection.insertOne(original);
        final SystemNotificationDto result = collection.find().first();

        assertThat(result).isNotNull();
        assertThat(result.key()).isNull();
        assertThat(result.title()).isNull();
        assertThat(result.description()).isNull();
        assertThat(result.actor()).isNull();
        assertThat(result.lastChanged()).isNull();
        assertThat(result.isRead()).isFalse();
        assertThat(result.triggeredAt()).isEqualTo(triggeredAt);
    }

    @Test
    void instantFieldsStoredAsBsonDate() {
        final Instant triggeredAt = Instant.parse("2026-04-20T08:15:00Z");
        final Instant lastChanged = Instant.parse("2026-04-21T10:30:00Z");

        final SystemNotificationDto dto = SystemNotificationDto.builder()
                .type("es_unavailable")
                .severity("normal")
                .nodeId("node-1")
                .details(Map.of())
                .isRead(true)
                .lastChanged(lastChanged)
                .triggeredAt(triggeredAt)
                .build();

        collection.insertOne(dto);

        final Document raw = mongoCollections.mongoConnection().getMongoDatabase()
                .getCollection(COLLECTION_NAME)
                .find()
                .first();

        assertThat(raw).isNotNull();
        assertThat(raw.get("triggered_at")).isInstanceOf(Date.class);
        assertThat(((Date) raw.get("triggered_at")).toInstant()).isEqualTo(triggeredAt);
        assertThat(raw.get("last_changed")).isInstanceOf(Date.class);
        assertThat(((Date) raw.get("last_changed")).toInstant()).isEqualTo(lastChanged);
    }

    @Test
    void actorSubdocumentStoredCorrectly() {
        final SystemNotificationDto dto = SystemNotificationDto.builder()
                .type("es_unavailable")
                .severity("normal")
                .nodeId("node-1")
                .details(Map.of())
                .isRead(true)
                .actor(SystemNotificationDto.Actor.create("64f2a1b3c8e9d0001a2b3c4d", "Jane Doe"))
                .triggeredAt(Instant.now())
                .build();

        collection.insertOne(dto);

        final Document raw = mongoCollections.mongoConnection().getMongoDatabase()
                .getCollection(COLLECTION_NAME)
                .find()
                .first();

        assertThat(raw).isNotNull();
        final Document actorDoc = raw.get("actor", Document.class);
        assertThat(actorDoc).isNotNull();
        assertThat(actorDoc.getString("id")).isEqualTo("64f2a1b3c8e9d0001a2b3c4d");
        assertThat(actorDoc.getString("name")).isEqualTo("Jane Doe");
    }

}
