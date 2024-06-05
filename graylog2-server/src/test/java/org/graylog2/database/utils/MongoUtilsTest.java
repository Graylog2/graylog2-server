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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedId;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.database.utils.MongoUtils.stringIdsIn;

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
        collection = mongoCollections.collection("test", DTO.class);
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
        final var rawCollection = mongoCollections.nonEntityCollection("raw_bson_test", RawBsonDocument.class);
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

    @Test
    void testIdsIn() {
        final String missingId1 = "6627add0ee216425dd6df36a";
        final String missingId2 = "6627add0ee216425dd6df36b";
        final String idA = "6627add0ee216425dd6df37a";
        final String idB = "6627add0ee216425dd6df37b";
        final String idC = "6627add0ee216425dd6df37c";
        final String idD = "6627add0ee216425dd6df37d";
        final String idE = "6627add0ee216425dd6df37e";
        final String idF = "6627add0ee216425dd6df37f";
        final var a = new DTO(idA, "a");
        final var b = new DTO(idB, "b");
        final var c = new DTO(idC, "c");
        final var d = new DTO(idD, "d");
        final var e = new DTO(idE, "e");
        final var f = new DTO(idF, "f");
        collection.insertMany(List.of(a, b, c, d, e, f));

        assertThat(collection.find(stringIdsIn(Set.of(idA, idF)))).contains(a, f);
        assertThat(collection.find(stringIdsIn(Set.of(idA, idF)))).hasSize(2);
        assertThat(collection.find(stringIdsIn(Set.of(missingId1, missingId2)))).hasSize(0);
        assertThat(collection.find(stringIdsIn(Set.of(idA, idB, missingId1, missingId2)))).contains(a, b);
        assertThat(collection.find(stringIdsIn(Set.of(idA, idB, missingId1, missingId2)))).hasSize(2);
        assertThat(collection.find(stringIdsIn(Set.of(idA, idB, idC, idD, idE, idF)))).hasSize(6);
        assertThat(collection.find(stringIdsIn(Set.of(idA, idB, idC, idD, idE, idF)))).contains(a, b, c, d, e, f);
        assertThat(collection.find(stringIdsIn(Set.of(idA, idB, idC, idD, idE, idF, missingId1, missingId2)))).hasSize(6);
        assertThat(collection.find(stringIdsIn(Set.of(idA, idB, idC, idD, idE, idF, missingId1, missingId2)))).contains(a, b, c, d, e, f);
    }
}
