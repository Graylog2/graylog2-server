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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonType;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.CommonMongoJackObjectMapperProvider;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class MongoCollectionsTest {

    private MongoCollections collections;
    private EncryptedValueService encryptedValueService;

    record Person(@JsonProperty("id") @Id @ObjectId String id,
                  @JsonProperty("external_id") @ObjectId String externalId,
                  @JsonProperty("first_name") String firstName,
                  @JsonProperty("created_at") ZonedDateTime createdAt,
                  @JsonProperty("last_modified_at") DateTime lastModifiedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Secret(@JsonProperty("encrypted_value") EncryptedValue encryptedValue) {}

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService) {
        collections = new MongoCollections(new CommonMongoJackObjectMapperProvider(new ObjectMapperProvider()), mongoDBTestService.mongoConnection());
        encryptedValueService = new EncryptedValueService(UUID.randomUUID().toString());
    }

    @Test
    void testBasicTypes() {
        final MongoCollection<Person> collection = collections.get("people", Person.class);
        final Person person = new Person(
                "000000000000000000000001",
                "000000000000000000000002",
                "Gary",
                ZonedDateTime.now(ZoneOffset.UTC).withNano(0),
                DateTime.now(DateTimeZone.UTC).withMillisOfSecond(0));

        final InsertOneResult insertOneResult = collection.insertOne(person);
        assertThat(insertOneResult.getInsertedId()).isNotNull().satisfies(bson ->
                assertThat(bson.asObjectId().getValue().toHexString()).isEqualTo(person.id()));

        assertThat(collection.find()).hasSize(1).allMatch(person::equals);
        assertThat(collection.find(Filters.eq("_id", person.id()))).hasSize(1);
        assertThat(collection.find(Filters.type("external_id", BsonType.OBJECT_ID))).hasSize(1);
        assertThat(collection.find(Filters.type("first_name", BsonType.STRING))).hasSize(1);
        assertThat(collection.find(Filters.type("created_at", BsonType.DATE_TIME))).hasSize(1);
        assertThat(collection.find(Filters.type("last_modified_at", BsonType.DATE_TIME))).hasSize(1);
    }

    @Test
    void testEncryptedValue() {
        final MongoCollection<Secret> collection = collections.get("secrets", Secret.class);
        final EncryptedValue encryptedValue = encryptedValueService.encrypt("gary");
        collection.insertOne(new Secret(encryptedValue));
        assertThat(collection.find().first()).isNotNull().satisfies(secret -> {
            final EncryptedValue readValue = secret.encryptedValue();
            assertThat(readValue.isSet()).isTrue();
            assertThat(encryptedValueService.decrypt(readValue)).isEqualTo("gary");
        });
    }

}
