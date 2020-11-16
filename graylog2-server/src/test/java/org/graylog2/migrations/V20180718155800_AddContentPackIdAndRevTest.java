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
package org.graylog2.migrations;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.model.ContentPack;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.exists;
import static org.assertj.core.api.Assertions.assertThat;

public class V20180718155800_AddContentPackIdAndRevTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private V20180718155800_AddContentPackIdAndRev migration;

    @Before
    public void setUp() {
        this.migration = new V20180718155800_AddContentPackIdAndRev(mongodb.mongoConnection());
    }

    @Test
    public void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2018-07-18T15:58:00Z"));
    }

    @Test
    @MongoDBFixtures("V20180718155800_AddContentPackIdAndRevTest.json")
    public void upgrade() {
        final MongoCollection<Document> collection = mongodb.mongoConnection()
                .getMongoDatabase()
                .getCollection(ContentPackPersistenceService.COLLECTION_NAME);
        final Bson filter = and(exists(ContentPack.FIELD_META_ID), exists(ContentPack.FIELD_META_REVISION));

        assertThat(collection.count(filter)).isEqualTo(1L);
        migration.upgrade();
        assertThat(collection.count(filter)).isEqualTo(2L);
    }
}
