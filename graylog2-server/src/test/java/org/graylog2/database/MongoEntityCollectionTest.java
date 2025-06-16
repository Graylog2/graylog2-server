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
package org.graylog2.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class MongoEntityCollectionTest {
    record Entity(@JsonProperty("id") @Id @ObjectId String id,
                  @JsonProperty("title") String title) implements MongoEntity {
    }

    MongoCollection<Entity> collection;

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider mongoJackObjectMapperProvider) {
        final var collections = new MongoCollections(mongoJackObjectMapperProvider, mongoDBTestService.mongoConnection());

        this.collection = collections.collection("mongo_collection_test", Entity.class);
    }

    @Test
    void getOrCreate() {
        final var id = new org.bson.types.ObjectId();
        final var idString = id.toHexString();
        final var dto = new Entity(idString, "Test");

        assertThat(collection.find(eq("_id", id)).first()).isNull();

        assertThat(collection.getOrCreate(dto)).satisfies(result -> {
            assertThat(result.id()).isEqualTo(idString);
            assertThat(result.title()).isEqualTo("Test");
            assertThat(result).isEqualTo(dto);
        });

        assertThat(collection.find(eq("_id", id)).first()).isNotNull().satisfies(result -> {
            assertThat(result.id()).isEqualTo(idString);
            assertThat(result.title()).isEqualTo("Test");
            assertThat(result).isEqualTo(dto);
        });

        // Using a different name in the DTO doesn't update the existing entry in the collection
        assertThat(collection.getOrCreate(new Entity(idString, "Another"))).satisfies(result -> {
            assertThat(result.id()).isEqualTo(idString);
            assertThat(result.title()).isEqualTo("Test");
            assertThat(result).isEqualTo(dto);
        });
    }

    @Test
    void getOrCreateWithNullEntity() {
        assertThatThrownBy(() -> collection.getOrCreate(null))
                .hasMessageContaining("entity cannot be null")
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getOrCreateWithNullEntityID() {
        assertThatThrownBy(() -> collection.getOrCreate(new Entity(null, "Test")))
                .hasMessageContaining("entity ID cannot be null")
                .isInstanceOf(NullPointerException.class);
    }
}
