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
package org.graylog.collectors.db;

import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class EnrollmentTokenDTOTest {

    private MongoCollection<EnrollmentTokenDTO> collection;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        collection = mongoCollections.collection("tokens", EnrollmentTokenDTO.class);

    }

    @Test
    void serializesAndDeserializes() throws Exception {
        final var id = new ObjectId();
        final var clock = Clock.fixed(Instant.ofEpochMilli(123), ZoneOffset.UTC);
        final var now = Instant.now(clock);
        final var creator = new EnrollmentTokenCreator("user-1", "alice");
        final var dto = new EnrollmentTokenDTO(
                id.toHexString(),
                "token-name-1",
                "jti-abc",
                "kid-abc",
                "fleet-1",
                creator,
                now,
                now.plusSeconds(3600),
                5,
                now.minusSeconds(60)
        );

        collection.insertOne(dto);

        final var deserialized = collection.find(Filters.eq("_id", id)).first();

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.id()).isEqualTo(id.toHexString());
        assertThat(deserialized.name()).isEqualTo("token-name-1");
        assertThat(deserialized.jti()).isEqualTo("jti-abc");
        assertThat(deserialized.kid()).isEqualTo("kid-abc");
        assertThat(deserialized.fleetId()).isEqualTo("fleet-1");
        assertThat(deserialized.createdBy().userId()).isEqualTo("user-1");
        assertThat(deserialized.createdBy().username()).isEqualTo("alice");
        assertThat(deserialized.createdAt()).isEqualTo(now);
        assertThat(deserialized.expiresAt()).isEqualTo(now.plusSeconds(3600));
        assertThat(deserialized.usageCount()).isEqualTo(5);
        assertThat(deserialized.lastUsedAt()).isEqualTo(now.minusSeconds(60));
    }

    @Test
    void serializesWithNullExpiresAtAndLastUsedAt() throws Exception {
        final var id = new ObjectId();
        final var clock = Clock.fixed(Instant.ofEpochMilli(123), ZoneOffset.UTC);
        final var now = Instant.now(clock);
        final var creator = new EnrollmentTokenCreator("user-2", "bob");
        final var dto = new EnrollmentTokenDTO(
                id.toHexString(),
                "token-name-2",
                "jti-xyz",
                "kid-xyz",
                "fleet-2",
                creator,
                now,
                null,
                0,
                null
        );

        collection.insertOne(dto);

        final var deserialized = collection.find(Filters.eq("_id", id)).first();

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.expiresAt()).isNull();
        assertThat(deserialized.lastUsedAt()).isNull();
        assertThat(deserialized.usageCount()).isZero();
    }
}
