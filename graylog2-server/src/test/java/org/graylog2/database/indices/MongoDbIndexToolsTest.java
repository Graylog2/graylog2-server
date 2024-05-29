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
package org.graylog2.database.indices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.testing.ObjectMapperExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(ObjectMapperExtension.class)
class MongoDbIndexToolsTest {
    private static final String COLLECTION_NAME = "test";

    private MongoDbIndexTools<Document> toTest;
    private MongoCollection<Document> db;
    private MongoCollection<Document> rawdb;

    @BeforeEach
    void setUp(MongoDBTestService mongodb, ObjectMapper objectMapper) {
        mongodb.mongoCollection(COLLECTION_NAME).drop();
        final MongoCollections mongoCollections = new MongoCollections(new MongoJackObjectMapperProvider(objectMapper), mongodb.mongoConnection());
        this.rawdb = mongoCollections.get(COLLECTION_NAME, Document.class);
        this.db = spy(this.rawdb);
        toTest = new MongoDbIndexTools<>(db);
    }

    @Test
    void throwsExceptionIfAnyStringSortFieldIsNotPresentOnTheListOfAllSortFields() {
        assertThrows(IllegalArgumentException.class, () -> toTest.prepareIndices("id", List.of("id", "number"), List.of("title")));
    }

    @Test
    void doesNotCreateIndexForId() {
        toTest.prepareIndices("id", List.of("id"), List.of());

        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void doesNotCreateSimpleIndexIfProperOneExists() {
        rawdb.createIndex(Indexes.ascending("number"));

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void doesNotCreateCollationIndexIfProperOneExists() {
        rawdb.createIndex(Indexes.ascending("summary"), new IndexOptions().collation(Collation.builder().locale("en").build()));

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void createsSimpleIndexIfDoesNotExists() {
        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db).createIndex(eq(Indexes.ascending("number")), argThat(indexOptions -> !indexOptions.isUnique()));
    }

    @Test
    void createsCollationIndexIfDoesNotExists() {
        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db).createIndex(eq(Indexes.ascending("summary")), argThat(indexOptions -> indexOptions.getCollation().getLocale().equals("en")));
    }

    @Test
    void replacesWrongCollationIndexWithProperOne() {
        //number should not have collation index, but a simple one!
        rawdb.createIndex(Indexes.ascending("number"), new IndexOptions().collation(Collation.builder().locale("en").collationStrength(CollationStrength.TERTIARY).build()));

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db).dropIndex(Indexes.ascending("number"));
        verify(db).createIndex(eq(Indexes.ascending("number")), argThat(indexOptions -> !indexOptions.isUnique()));
    }

    @Test
    void replacesWrongSimpleIndexWithProperOne() {
        //summary should not have collation index, but a simple one!
        rawdb.createIndex(Indexes.ascending("summary"));

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db).dropIndex(Indexes.ascending("summary"));
        verify(db).createIndex(eq(Indexes.ascending("summary")), argThat(indexOptions -> indexOptions.getCollation().getLocale().equals("en")));
    }
}
