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
package org.graylog.plugins.views.search.jobs;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJobIdentifier;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


public class SearchJobStateServiceTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private SearchJobStateService toTest;

    @Before
    public void setUp() {
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        this.toTest = new SearchJobStateService(new MongoCollections(objectMapperProvider, mongoConnection));
    }

    @Test
    public void testSaveAndGet() {
        final SearchJobState toBeSaved = SearchJobState.builder()
                .identifier(new SearchJobIdentifier("777fd86ae6db8b71a8e10000", "677fd86ae6db8b71a8e10e3e", "john", "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RUNNING)
                .progress(42)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
        toTest.create(toBeSaved);
        final Optional<SearchJobState> retrieved = toTest.get("777fd86ae6db8b71a8e10000");
        assertTrue(retrieved.isPresent());
        assertEquals(toBeSaved.toBuilder()
                .identifier(new SearchJobIdentifier("777fd86ae6db8b71a8e10000", "677fd86ae6db8b71a8e10e3e", "john", "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .build(), retrieved.get());
    }

    @Test
    public void testGetExecutionState() {
        final SearchJobState toBeSaved = SearchJobState.builder()
                .identifier(new SearchJobIdentifier("777fd86ae6db8b71a8e10000", "677fd86ae6db8b71a8e10e3e", "john", "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RUNNING)
                .progress(42)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
        toTest.create(toBeSaved);
        final Optional<SearchJobExecutionState> retrieved = toTest.getExecutionState("777fd86ae6db8b71a8e10000");
        assertTrue(retrieved.isPresent());
        assertEquals(new SearchJobExecutionState(toBeSaved.status(), toBeSaved.progress()), retrieved.get());
    }

    @Test
    public void testSaveAndDelete() {
        final SearchJobState toBeSaved = SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null, "677fd86ae6db8b71a8e10e3e", "john", "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RUNNING)
                .progress(42)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
        final SearchJobState saved = toTest.create(toBeSaved);
        final Optional<SearchJobState> retrieved = toTest.get(saved.id());
        assertTrue(retrieved.isPresent());
        assertTrue(toTest.delete(saved.id()));
        assertTrue(toTest.get(saved.id()).isEmpty());
    }

    @Test
    public void testSaveAndUpdate() {
        final SearchJobState toBeSaved = SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null, "677fd86ae6db8b71a8e10e3e", "john", "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RUNNING)
                .progress(42)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
        final SearchJobState saved = toTest.create(toBeSaved);
        Optional<SearchJobState> retrieved = toTest.get(saved.id());
        assertTrue(retrieved.isPresent());

        final boolean updated = toTest.update(saved.toBuilder().progress(77).build());
        assertTrue(updated);
        retrieved = toTest.get(saved.id());
        assertTrue(retrieved.isPresent());
        assertEquals(77, retrieved.get().progress());
        assertTrue(retrieved.get().updatedAt().isAfter(toBeSaved.updatedAt()));
    }

    @Test
    public void testChangeStatus() {
        final SearchJobState toBeSaved = SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null, "677fd86ae6db8b71a8e10e3e", "john", "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RUNNING)
                .progress(42)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
        final SearchJobState saved = toTest.create(toBeSaved);
        Optional<SearchJobState> retrieved = toTest.get(saved.id());
        assertTrue(retrieved.isPresent());

        final boolean updated = toTest.changeStatus(saved.identifier().id(), SearchJobStatus.DONE);
        assertTrue(updated);
        retrieved = toTest.get(saved.id());
        assertTrue(retrieved.isPresent());
        assertEquals(SearchJobStatus.DONE, retrieved.get().status());
        assertTrue(retrieved.get().updatedAt().isAfter(toBeSaved.updatedAt()));
    }

    @Test
    public void testResetJobsAreImmuneToUpdates() {
        final SearchJobState toBeSaved = SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null, "677fd86ae6db8b71a8e10e3e", "john", "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RESET)
                .progress(42)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
        final SearchJobState saved = toTest.create(toBeSaved);

        assertFalse(toTest.changeStatus(saved.id(), SearchJobStatus.ERROR));
        assertFalse(toTest.update(saved.toBuilder().progress(13).build()));
    }

    @Test
    public void testReset() {
        final SearchJobState savedSearchJobState = toTest.create(SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null,
                        "677fd86ae6db8b71a8e10001",
                        "john",
                        "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(createSimpleQueryResult("0000000000000042"))
                .errors(Set.of())
                .status(SearchJobStatus.RUNNING)
                .progress(42)
                .createdAt(DateTime.parse("1999-01-01T11:11:11"))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build());

        assertTrue(toTest.resetLatestForUser("jose").isEmpty()); //no active query for Jose

        final Optional<SearchJobState> previousState = toTest.resetLatestForUser("john");
        assertTrue(previousState.isPresent());
        assertEquals(savedSearchJobState, previousState.get());
        final Optional<SearchJobState> latestForJohn = toTest.getLatestForUser("john");
        assertTrue(latestForJohn.isPresent());
        assertEquals(SearchJobStatus.RESET, latestForJohn.get().status());
        assertEquals(savedSearchJobState.identifier(), latestForJohn.get().identifier());
        assertNull(latestForJohn.get().result());
        assertEquals(Set.of(), latestForJohn.get().errors());
    }

    @Test
    public void testGetLatestForUser() {

        toTest.create(SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null,
                        "677fd86ae6db8b71a8e10001",
                        "john",
                        "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RUNNING)
                .progress(42)
                .createdAt(DateTime.parse("1999-01-01T11:11:11"))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build());

        toTest.create(SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null,
                        "677fd86ae6db8b71a8e10002",
                        "john",
                        "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RUNNING)
                .progress(42)
                .createdAt(DateTime.parse("2000-01-01T11:11:11"))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build());

        toTest.create(SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null,
                        "677fd86ae6db8b71a8e10003",
                        "bob",
                        "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RUNNING)
                .progress(42)
                .createdAt(DateTime.parse("2020-01-01T11:11:11"))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build());

        assertTrue(toTest.getLatestForUser("andy").isEmpty()); //Andy has no search jobs
        assertEquals("677fd86ae6db8b71a8e10003", toTest.getLatestForUser("bob").get().identifier().searchId()); //Bob has only one, we choose it
        assertEquals("677fd86ae6db8b71a8e10002", toTest.getLatestForUser("john").get().identifier().searchId()); //John has 2, we choose the one with the latest "created at" date

    }

    @Test
    public void testOldJobsRemovalAndExpiration() {
        final SearchJobState oldJob = toTest.create(SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null,
                        "677fd86ae6db8b71a8e10001",
                        "john",
                        "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.DONE)
                .progress(42)
                .createdAt(DateTime.parse("1999-01-01T11:11:11"))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build());
        final SearchJobState alreadyExpiredJob = toTest.create(SearchJobState.builder()
                .identifier(new SearchJobIdentifier("7777786ae6db8b71a8e10002",
                        "677fd86ae6db8b71a8e10002",
                        "john",
                        "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(createSimpleQueryResult("0000000000000042"))
                .status(SearchJobStatus.EXPIRED)
                .progress(100)
                .createdAt(DateTime.now(DateTimeZone.UTC).minusDays(1))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build());
        final SearchJobState jobThatNeedsToBeExpired = toTest.create(SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null,
                        "677fd86ae6db8b71a8e10002",
                        "john",
                        "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.DONE)
                .progress(42)
                .createdAt(DateTime.now(DateTimeZone.UTC).minusDays(1))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build());
        final SearchJobState freshJob = toTest.create(SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null,
                        "677fd86ae6db8b71a8e10003",
                        "john",
                        "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.DONE)
                .progress(42)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build());

        long numRemoved = toTest.deleteOlderThan(DateTime.now(DateTimeZone.UTC).minusDays(2));
        assertEquals(1, numRemoved);
        assertTrue(toTest.get(oldJob.id()).isEmpty());
        assertTrue(toTest.get(alreadyExpiredJob.id()).isPresent());
        assertTrue(toTest.get(jobThatNeedsToBeExpired.id()).isPresent());
        assertTrue(toTest.get(freshJob.id()).isPresent());

        long numExpired = toTest.expireOlderThan(DateTime.now(DateTimeZone.UTC).minusHours(7));
        assertEquals(1, numExpired);
        final SearchJobState expiredSearchJobState = toTest.get(jobThatNeedsToBeExpired.id()).get();
        assertSame(expiredSearchJobState.status(), SearchJobStatus.EXPIRED);
        assertTrue(expiredSearchJobState.result() == null || expiredSearchJobState.result().searchTypes() == null || expiredSearchJobState.result().searchTypes().isEmpty());
        assertSame(toTest.get(freshJob.id()).get().status(), SearchJobStatus.DONE);

        numRemoved = toTest.deleteOlderThan(DateTime.now(DateTimeZone.UTC).minusDays(2));
        assertEquals(0, numRemoved);
    }

    @Test
    public void testChangeProgress() {
        final SearchJobState newJob = toTest.create(SearchJobState.builder()
                .identifier(new SearchJobIdentifier(null,
                        "677fd86ae6db8b71a8e10001",
                        "john",
                        "dcae52e4-777e-4e3f-8e69-61df7a607016"))
                .result(noResult("0000000000000042"))
                .status(SearchJobStatus.RUNNING)
                .progress(0)
                .createdAt(DateTime.parse("1999-01-01T11:11:11"))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build());

        boolean done = toTest.changeProgress(newJob.identifier().id(), 1);
        assertTrue(done);
        Optional<SearchJobState> retrieved = toTest.get(newJob.identifier().id());
        assertTrue(retrieved.isPresent());
        assertEquals(SearchJobStatus.RUNNING, retrieved.get().status());
        assertEquals(1, retrieved.get().progress());
        assertEquals("0000000000000042", retrieved.get().result().query().id());

        done = toTest.changeProgress(newJob.identifier().id(), 2);
        assertTrue(done);
        retrieved = toTest.get(newJob.identifier().id());
        assertTrue(retrieved.isPresent());
        assertEquals(SearchJobStatus.RUNNING, retrieved.get().status());
        assertEquals(2, retrieved.get().progress());
        assertEquals("0000000000000042", retrieved.get().result().query().id());

        final QueryResult simpleQueryResult = createSimpleQueryResult("0000000000000077");
        done = toTest.changeProgress(newJob.identifier().id(), 50, simpleQueryResult, null);
        assertTrue(done);
        retrieved = toTest.get(newJob.identifier().id());
        assertTrue(retrieved.isPresent());
        assertEquals(SearchJobStatus.RUNNING, retrieved.get().status());
        assertEquals(50, retrieved.get().progress());
        assertEquals("0000000000000077", retrieved.get().result().query().id());

        final QueryResult finalSimpleQueryResult = createSimpleQueryResult("0000000000000099");
        done = toTest.changeProgress(newJob.identifier().id(), 100, finalSimpleQueryResult, SearchJobStatus.DONE);
        assertTrue(done);
        retrieved = toTest.get(newJob.identifier().id());
        assertTrue(retrieved.isPresent());
        assertEquals(SearchJobStatus.DONE, retrieved.get().status());
        assertEquals(100, retrieved.get().progress());
        assertEquals("0000000000000099", retrieved.get().result().query().id());
    }

    private QueryResult createSimpleQueryResult(final String id) {
        return QueryResult.builder()
                .query(Query.builder()
                        .id(id)
                        .timerange(KeywordRange.create("last year", "UTC"))
                        .query(ElasticsearchQueryString.empty())
                        .searchTypes(Set.of())
                        .build())
                .searchTypes(Map.of())
                .build();
    }

    private QueryResult noResult(final String id) {
        return QueryResult.builder()
                .searchTypes(Collections.emptyMap())
                .query(Query.builder()
                        .id(id)
                        .timerange(KeywordRange.create("last year", "UTC"))
                        .query(ElasticsearchQueryString.empty())
                        .build()).build();
    }
}
