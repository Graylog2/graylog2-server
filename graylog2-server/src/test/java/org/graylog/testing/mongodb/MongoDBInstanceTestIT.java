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
package org.graylog.testing.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoDBInstanceTestIT {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private MongoCollection<Document> collection1;
    private MongoCollection<Document> collection2;

    @Before
    public void setUp() throws Exception {
        collection1 = mongodb.mongoConnection().getMongoDatabase().getCollection("test_1");
        collection2 = mongodb.mongoConnection().getMongoDatabase().getCollection("test_2");
    }

    @Test
    public void clientWorks() {
        assertThat(mongodb.mongoConnection()).isNotNull();
        assertThat(mongodb.mongoConnection().getMongoDatabase()).isNotNull();
        assertThat(mongodb.mongoConnection().getMongoDatabase().getName()).isEqualTo("graylog");

        final Document document = new Document("hello", "world");

        collection1.insertOne(document);

        assertThat(collection1.count()).isEqualTo(1);
        assertThat(collection1.find(Filters.eq("hello", "world")).first()).isEqualTo(document);
        assertThat(collection1.find(Filters.eq("hello", "world2")).first()).isNull();
    }

    @Test
    @MongoDBFixtures("MongoDBBaseTestIT.json")
    public void fixturesWork() {
        assertThat(collection1.count()).isEqualTo(2);
        assertThat(collection1.find(Filters.eq("hello", "world")).first().get("_id"))
                .isEqualTo(new ObjectId("54e3deadbeefdeadbeefaffe"));
        assertThat(collection1.find(Filters.eq("hello", "world2")).first()).isNull();
        assertThat(collection1.find(Filters.eq("another", "test")).first().get("_id"))
                .isEqualTo(new ObjectId("54e3deadbeefdeadbeefafff"));

        assertThat(collection2.count()).isEqualTo(1);
        assertThat(collection2.find(Filters.eq("field_a", "content1")).first().get("_id"))
                .isEqualTo(new ObjectId("54e3deadbeefdeadbeefaffe"));
        assertThat(collection2.find(Filters.eq("field_a", "missing")).first()).isNull();

        final Date date = new Date(ZonedDateTime.parse("2018-12-31T23:59:59.999Z").toInstant().toEpochMilli());
        assertThat(collection2.find(Filters.gt("created_at", date)).first().get("_id"))
                .isEqualTo(new ObjectId("54e3deadbeefdeadbeefaffe"));
        assertThat(collection2.find(Filters.lte("created_at", date)).first()).isNull();
    }

    @Test
    @MongoDBFixtures("MongoDBBaseTestIT.json")
    public void indexFixturesWork() {
        final List<Document> indexes = StreamSupport.stream(collection2.listIndexes().spliterator(), false)
                .collect(Collectors.toList());

        assertThat(indexes.get(1).get("key", Document.class).getInteger("field_a")).isEqualTo(1);
        assertThat(indexes.get(1).getBoolean("unique")).isEqualTo(true);
        assertThat(indexes.get(2).get("key", Document.class).getInteger("created_at")).isEqualTo(-1);
        assertThat(indexes.get(2).getBoolean("unique")).isNull();
    }

    @Test
    @MongoDBFixtures("MongoDBBaseTestIT.json")
    public void globalFixturesWork() {
        assertThat(collection1.count()).isEqualTo(2);
        assertThat(collection1.find(Filters.eq("hello", "world")).first().get("_id"))
                .isEqualTo(new ObjectId("54e3deadbeefdeadbeefaffe"));
        assertThat(collection1.find(Filters.eq("hello", "world2")).first()).isNull();
        assertThat(collection1.find(Filters.eq("another", "test")).first().get("_id"))
                .isEqualTo(new ObjectId("54e3deadbeefdeadbeefafff"));
    }
}
