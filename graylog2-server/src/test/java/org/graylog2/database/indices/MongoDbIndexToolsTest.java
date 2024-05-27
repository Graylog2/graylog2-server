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

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MongoDbIndexToolsTest {

    private MongoDbIndexTools toTest;
    @Mock
    private MongoCollection<?> db;

    @BeforeEach
    void setUp() {
        toTest = new MongoDbIndexTools(db);
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
        doReturn(List.of(BasicDBObject.parse("""
                {v: 2, key: { number: 1 }, name: 'number_1'}
                """)))
                .when(db).listIndexes();

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void doesNotCreateCollationIndexIfProperOneExists() {
        doReturn(List.of(BasicDBObject.parse("""
                               {
                                 v: 2,
                                 key: { summary: 1 },
                                 name: 'summary_1',
                                 collation: {
                                   locale: 'en',
                                   strength: 3

                                 }
                               }
                """)))
                .when(db).listIndexes();

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void createsSimpleIndexIfDoesNotExists() {
        doReturn(List.of()).when(db).listIndexes();

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db).createIndex(Indexes.ascending("number"), new IndexOptions().unique(false));
    }

    @Test
    void createsCollationIndexIfDoesNotExists() {
        doReturn(List.of()).when(db).listIndexes();

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db).createIndex(Indexes.ascending("summary"), new IndexOptions().collation(Collation.builder().locale("en").build()));
    }

    @Test
    void replacesWrongCollationIndexWithProperOne() {
        //number should not have collation index, but a simple one!
        doReturn(List.of(BasicDBObject.parse("""
                               {
                                 v: 2,
                                 key: { number: 1 },
                                 name: 'number_1',
                                 collation: {
                                   locale: 'en',
                                   strength: 3

                                 }
                               }
                """)))
                .when(db).listIndexes();

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db).dropIndex(Indexes.ascending("number"));
        verify(db).createIndex(Indexes.ascending("number"), new IndexOptions().unique(false));
    }

    @Test
    void replacesWrongSimpleIndexWithProperOne() {
        //summary should not have collation index, but a simple one!
        doReturn(List.of(BasicDBObject.parse("{v: 2, key: { summary: 1 }, name: 'summary_1'}))")))
                .when(db).listIndexes();

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db).dropIndex(Indexes.ascending("summary"));
        verify(db).createIndex(Indexes.ascending("summary"), new IndexOptions().collation(Collation.builder().locale("en").build()));
    }
}
