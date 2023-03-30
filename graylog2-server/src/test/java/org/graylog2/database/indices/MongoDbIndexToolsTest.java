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
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mongojack.JacksonDBCollection;

import java.util.List;

import static org.graylog2.database.indices.MongoDbIndexTools.COLLATION_KEY;
import static org.graylog2.database.indices.MongoDbIndexTools.LOCALE_KEY;
import static org.graylog2.database.indices.MongoDbIndexTools.UNIQUE_KEY;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MongoDbIndexToolsTest {

    private MongoDbIndexTools toTest;
    @Mock
    private JacksonDBCollection<?, ObjectId> db;

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
        verify(db, never()).createIndex(any(), any());
    }

    @Test
    void doesNotCreateSimpleIndexIfProperOneExists() {
        doReturn(List.of(BasicDBObject.parse("""
                {v: 2, key: { number: 1 }, name: 'number_1'}
                """)))
                .when(db).getIndexInfo();

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(), any());
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
                .when(db).getIndexInfo();

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(), any());
    }

    @Test
    void createsSimpleIndexIfDoesNotExists() {
        doReturn(List.of()).when(db).getIndexInfo();

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db).createIndex(new BasicDBObject("number", 1), new BasicDBObject(UNIQUE_KEY, false));
    }

    @Test
    void createsCollationIndexIfDoesNotExists() {
        doReturn(List.of()).when(db).getIndexInfo();

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db).createIndex(new BasicDBObject("summary", 1), new BasicDBObject(COLLATION_KEY, new BasicDBObject(LOCALE_KEY, "en")));
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
                .when(db).getIndexInfo();

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db).dropIndex(new BasicDBObject("number", 1));
        verify(db).createIndex(new BasicDBObject("number", 1), new BasicDBObject(UNIQUE_KEY, false));
    }

    @Test
    void replacesWrongSimpleIndexWithProperOne() {
        //summary should not have collation index, but a simple one!
        doReturn(List.of(BasicDBObject.parse("{v: 2, key: { summary: 1 }, name: 'summary_1'}))")))
                .when(db).getIndexInfo();

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db).dropIndex(new BasicDBObject("summary", 1));
        verify(db).createIndex(new BasicDBObject("summary", 1), new BasicDBObject(COLLATION_KEY, new BasicDBObject(LOCALE_KEY, "en")));
    }
}
