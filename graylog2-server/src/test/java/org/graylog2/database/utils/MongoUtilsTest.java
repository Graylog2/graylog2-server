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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClientException;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcernResult;
import com.mongodb.WriteError;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.RawBsonDocument;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mongojack.Id;

import java.util.Collections;
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

    private record DTO(@Id @org.mongojack.ObjectId String id, String name) implements MongoEntity {}

    private record DTORef(@Id @org.mongojack.ObjectId String id, ObjectId refId,
                          String name) implements MongoEntity {}

    private MongoCollections mongoCollections;
    private MongoCollection<DTO> collection;
    private MongoCollection<DTORef> collectionRef;
    private MongoUtils<DTO> utils;

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider objectMapperProvider) {
        mongoCollections = new MongoCollections(objectMapperProvider, mongoDBTestService.mongoConnection());
        collection = mongoCollections.collection("test", DTO.class);
        collectionRef = mongoCollections.collection("test1", DTORef.class);
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
    void testObjectIdEq() {
        final var a = new DTORef("6627add0ee216425dd6df37c", new ObjectId("6627add0ee216425dd6df37d"), "a");
        final var b = new DTORef("6627add0ee216425dd6df37d", new ObjectId("6627add0ee216425dd6df37c"), "b");
        collectionRef.insertMany(List.of(a, b));

        assertThat(collectionRef.find(MongoUtils.objectIdEq("ref_id", a.id())).first()).isEqualTo(b);
        assertThat(collectionRef.find(MongoUtils.objectIdEq("ref_id", new ObjectId("6627add0ee216425dd6df37d"))).first()).isEqualTo(a);
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

    @Test
    void testGetOrCreate() {
        final var id = new ObjectId().toHexString();
        final var dto = new DTO(id, "test");

        assertThat(utils.getById(id)).isEmpty();

        assertThat(utils.getOrCreate(dto)).satisfies(result -> {
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("test");
            assertThat(result).isEqualTo(dto);
        });

        assertThat(utils.getById(id)).isPresent().get().satisfies(result -> {
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("test");
            assertThat(result).isEqualTo(dto);
        });

        // Using a different name in the DTO doesn't update the existing entry in the collection
        assertThat(utils.getOrCreate(new DTO(id, "another"))).satisfies(result -> {
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("test");
            assertThat(result).isEqualTo(dto);
        });
    }

    @Test
    void testGetOrCreateWithNullEntity() {
        assertThatThrownBy(() -> utils.getOrCreate(null))
                .hasMessageContaining("entity cannot be null")
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testGetOrCreateWithNullEntityID() {
        assertThatThrownBy(() -> utils.getOrCreate(new DTO(null, "test")))
                .hasMessageContaining("entity ID cannot be null")
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testIsDuplicateKeyError() {
        final var clientException = new MongoClientException("Something went wrong!");
        final var madeUpServerException = new MongoWriteException(
                new WriteError(12345,
                        "E12345 some error that I just made up",
                        new BsonDocument()),
                new ServerAddress(), Collections.emptySet());
        final var dupKeyException = new MongoWriteException(
                new WriteError(11000,
                        "E11000 duplicate key error collection: graylog.example index: action_id_1 dup key: { foo_id: \"bar\" }",
                        new BsonDocument()),
                new ServerAddress(), Collections.emptySet());
        final var legacyDupKeyException = new DuplicateKeyException(
                new BsonDocument("err", new BsonString("E11000 duplicate key error collection: graylog.example index: action_id_1 dup key: { foo_id: \"bar\" }")),
                new ServerAddress(), WriteConcernResult.acknowledged(0, false, null));

        assertThat(MongoUtils.isDuplicateKeyError(clientException)).isFalse();
        assertThat(MongoUtils.isDuplicateKeyError(madeUpServerException)).isFalse();
        assertThat(MongoUtils.isDuplicateKeyError(dupKeyException)).isTrue();
        assertThat(MongoUtils.isDuplicateKeyError(legacyDupKeyException)).isTrue();
    }

    @AutoValue
    @JsonDeserialize(builder = AutoValueDTO.Builder.class)
    public static abstract class AutoValueDTO implements BuildableMongoEntity<AutoValueDTO, AutoValueDTO.Builder> {
        @JsonProperty("name")
        public abstract String name();

        @Override
        public abstract Builder toBuilder();

        public static AutoValueDTO.Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder implements BuildableMongoEntity.Builder<AutoValueDTO, Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_MongoUtilsTest_AutoValueDTO.Builder();
            }

            @JsonProperty("name")
            public abstract Builder name(String name);

            public abstract AutoValueDTO build();
        }
    }

    @Test
    void testSaveAutoValueDTOWithoutId() {
        final var coll = mongoCollections.collection("autovalue-test", AutoValueDTO.class);
        final var util = mongoCollections.utils(coll);

        final var orig = AutoValueDTO.builder().name("test").build();
        assertThat(orig.id()).isNull();

        final var saved = util.save(orig);
        final var generatedId = saved.id();
        assertThat(saved)
                .isEqualTo(orig.toBuilder().id(generatedId).build())
                .isEqualTo(util.getById(generatedId).orElse(null));
    }

    @Test
    void testSaveAutoValueDTOWithExistingId() {
        final var coll = mongoCollections.collection("autovalue-test", AutoValueDTO.class);
        final var util = mongoCollections.utils(coll);

        final var existing = util.save(AutoValueDTO.builder().name("test").build());
        final var existingId = existing.id();
        assertThat(existingId).isNotNull();

        final var orig = existing.toBuilder().name("new name").build();
        final var saved = util.save(orig);

        assertThat(saved)
                .isEqualTo(orig)
                .isEqualTo(util.getById(existingId).orElse(null));
    }

    @Test
    void testSaveAutoValueDTOWithNewId() {
        final var coll = mongoCollections.collection("autovalue-test", AutoValueDTO.class);
        final var util = mongoCollections.utils(coll);

        final var orig = AutoValueDTO.builder().id(new ObjectId().toHexString()).name("test").build();
        assertThat(util.getById(orig.id())).isEmpty();

        final var saved = util.save(orig);
        assertThat(saved)
                .isEqualTo(orig)
                .isEqualTo(util.getById(orig.id()).orElse(null));
    }
}
