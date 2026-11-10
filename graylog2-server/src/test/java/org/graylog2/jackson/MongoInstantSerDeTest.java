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
package org.graylog2.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class MongoInstantSerDeTest {

    private static final String COLLECTION_NAME = "test_instant";

    @Test
    void instantIsStoredAsBsonDate(MongoCollections mongoCollections) {
        final Instant now = Instant.parse("2024-06-15T10:30:00Z");
        final var collection = mongoCollections.collection(COLLECTION_NAME, TestDTO.class);

        collection.insertOne(new TestDTO(null, now));

        // Read back as raw Document to verify the BSON type
        final Document raw = mongoCollections.mongoConnection().getMongoDatabase()
                .getCollection(COLLECTION_NAME)
                .find()
                .first();

        assertThat(raw).isNotNull();
        assertThat(raw.get("timestamp")).isInstanceOf(Date.class);
        assertThat(((Date) raw.get("timestamp")).toInstant()).isEqualTo(now);
    }

    @Test
    void instantRoundTripThroughMongoDB(MongoCollections mongoCollections) {
        final Instant now = Instant.parse("2024-06-15T10:30:00.123Z");
        final var collection = mongoCollections.collection(COLLECTION_NAME, TestDTO.class);

        collection.insertOne(new TestDTO(null, now));

        final TestDTO result = collection.find().first();

        assertThat(result).isNotNull();
        assertThat(result.timestamp()).isEqualTo(now);
    }

    @Test
    void nullInstantRoundTripThroughMongoDB(MongoCollections mongoCollections) {
        final var collection = mongoCollections.collection(COLLECTION_NAME, TestDTO.class);

        collection.insertOne(new TestDTO(null, null));

        final TestDTO result = collection.find().first();

        assertThat(result).isNotNull();
        assertThat(result.timestamp()).isNull();
    }

    record TestDTO(
            @Nullable
            @ObjectId
            @Id
            @JsonProperty("id")
            String id,

            @JsonSerialize(using = MongoInstantSerializer.class)
            @JsonDeserialize(using = MongoInstantDeserializer.class)
            @JsonProperty("timestamp")
            Instant timestamp
    ) implements MongoEntity {
    }
}
