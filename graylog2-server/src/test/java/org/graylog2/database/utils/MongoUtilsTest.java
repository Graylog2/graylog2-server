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
package org.graylog2.database.utils;

import com.mongodb.client.MongoCollection;
import org.bson.RawBsonDocument;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedId;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class MongoUtilsTest {

    private record DTO(String id, String name) implements MongoEntity {}

    private MongoCollections mongoCollections;
    private MongoCollection<DTO> collection;
    private MongoUtils<DTO> utils;

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider objectMapperProvider) {
        mongoCollections = new MongoCollections(objectMapperProvider, mongoDBTestService.mongoConnection());
        collection = mongoCollections.get("test", DTO.class);
        utils = mongoCollections.utils(collection);
    }

    @Test
    void testInsertedId() {
        final var id = "6627add0ee216425dd6df37c";
        final var a = new DTO(id, "a");

        assertThat(insertedId(collection.insertOne(a))).isEqualTo(new ObjectId(id));
        assertThat(insertedId(collection.insertOne(new DTO(null, "b")))).isGreaterThan(new ObjectId(id));
    }

    @Test
    void testInsertedIdAsString() {
        final var id = "6627add0ee216425dd6df37c";
        final var a = new DTO(id, "a");

        assertThat(insertedIdAsString(collection.insertOne(a))).isEqualTo(id);
        assertThat(insertedIdAsString(collection.insertOne(new DTO(null, "b")))).isNotBlank();
    }

    @Test
    void testNullInsertedId() {
        final var rawCollection = mongoCollections.get("raw_bson_test", RawBsonDocument.class);
        final RawBsonDocument doc = RawBsonDocument.parse("{\"name\":\"a\"}");
        assertThatThrownBy(() -> insertedId(rawCollection.insertOne(doc)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MongoEntity");
    }

    @Test
    void testIdEq() {
        final var a = new DTO("6627add0ee216425dd6df37c", "a");
        final var b = new DTO("6627add0ee216425dd6df37d", "b");
        collection.insertMany(List.of(a, b));

        assertThat(collection.find(idEq(a.id())).first()).isEqualTo(a);
        assertThat(collection.find(idEq(new ObjectId(a.id()))).first()).isEqualTo(a);
        assertThat(collection.find(idEq(b.id())).first()).isEqualTo(b);
        assertThat(collection.find(idEq(new ObjectId(b.id()))).first()).isEqualTo(b);
    }
}
