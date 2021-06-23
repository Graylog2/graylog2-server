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
package org.graylog2.indexer.ranges;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.assertj.jodatime.api.Assertions;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.events.IndicesClosedEvent;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.indices.events.IndicesReopenedEvent;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MongoIndexRangeServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    @Mock
    private Indices indices;
    @Mock
    private IndexSetRegistry indexSetRegistry;
    private EventBus localEventBus;
    private MongoIndexRangeService indexRangeService;

    @Before
    public void setUp() throws Exception {
        localEventBus = new EventBus("local-event-bus");
        indexRangeService = new MongoIndexRangeService(mongodb.mongoConnection(), objectMapperProvider, indices, indexSetRegistry, new NullAuditEventSender(), mock(NodeId.class), localEventBus);
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest.json")
    public void getReturnsExistingIndexRange() throws Exception {
        IndexRange indexRange = indexRangeService.get("graylog_1");

        assertThat(indexRange.indexName()).isEqualTo("graylog_1");
        assertThat(indexRange.begin()).isEqualTo(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        assertThat(indexRange.end()).isEqualTo(new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC));
        assertThat(indexRange.calculatedAt()).isEqualTo(new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC));
        assertThat(indexRange.calculationDuration()).isEqualTo(23);
    }

    @Test(expected = NotFoundException.class)
    @MongoDBFixtures("MongoIndexRangeServiceTest-LegacyIndexRanges.json")
    public void getIgnoresLegacyIndexRange() throws Exception {
        indexRangeService.get("graylog_0");
    }

    @Test(expected = NotFoundException.class)
    public void getThrowsNotFoundException() throws Exception {
        indexRangeService.get("does-not-exist");
    }

    /**
     * Test the following constellation:
     * <pre>
     *                        [-        index range       -]
     * [- graylog_1 -][- graylog_2 -][- graylog_3 -][- graylog_4 -][- graylog_5 -]
     * </pre>
     */
    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest-distinct.json")
    public void findReturnsIndexRangesWithinGivenRange() throws Exception {
        final DateTime begin = new DateTime(2015, 1, 2, 12, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2015, 1, 4, 12, 0, DateTimeZone.UTC);
        final SortedSet<IndexRange> indexRanges = indexRangeService.find(begin, end);

        assertThat(indexRanges).containsExactly(
                MongoIndexRange.create(new ObjectId("55e0261a0cc6980000000002"), "graylog_2", new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 3, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 3, 0, 0, DateTimeZone.UTC), 42),
                MongoIndexRange.create(new ObjectId("55e0261a0cc6980000000003"), "graylog_3", new DateTime(2015, 1, 3, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 4, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 4, 0, 0, DateTimeZone.UTC), 42),
                MongoIndexRange.create(new ObjectId("55e0261a0cc6980000000004"), "graylog_4", new DateTime(2015, 1, 4, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 5, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 5, 0, 0, DateTimeZone.UTC), 42)
        );
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest-LegacyIndexRanges.json")
    public void findIgnoresLegacyIndexRanges() throws Exception {
        when(indices.waitForRecovery("graylog_1")).thenReturn(HealthStatus.Green);

        final DateTime begin = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2015, 2, 1, 0, 0, DateTimeZone.UTC);
        final SortedSet<IndexRange> indexRanges = indexRangeService.find(begin, end);

        assertThat(indexRanges).containsOnly(
                MongoIndexRange.create(new ObjectId("55e0261a0cc6980000000003"), "graylog_1", new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC), 42)
        );
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest.json")
    public void findReturnsNothingBeforeBegin() throws Exception {
        final DateTime begin = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2016, 1, 2, 0, 0, DateTimeZone.UTC);
        Set<IndexRange> indexRanges = indexRangeService.find(begin, end);

        assertThat(indexRanges).isEmpty();
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest.json")
    public void findAllReturnsAllIndexRanges() throws Exception {
        assertThat(indexRangeService.findAll()).hasSize(2);
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest-LegacyIndexRanges.json")
    public void findAllReturnsAllIgnoresLegacyIndexRanges() throws Exception {
        assertThat(indexRangeService.findAll()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest.json")
    public void calculateRangeReturnsIndexRange() throws Exception {
        final String index = "graylog";
        final DateTime min = new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC);
        final DateTime max = new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC);
        when(indices.waitForRecovery(index)).thenReturn(HealthStatus.Green);
        when(indices.indexRangeStatsOfIndex(index)).thenReturn(IndexRangeStats.create(min, max));

        final IndexRange indexRange = indexRangeService.calculateRange(index);

        assertThat(indexRange.indexName()).isEqualTo(index);
        assertThat(indexRange.begin()).isEqualTo(min);
        assertThat(indexRange.end()).isEqualTo(max);
        Assertions.assertThat(indexRange.calculatedAt()).isEqualToIgnoringHours(DateTime.now(DateTimeZone.UTC));
    }

    @Test(expected = ElasticsearchException.class)
    public void calculateRangeFailsIfIndexIsNotHealthy() throws Exception {
        final String index = "graylog";
        when(indices.waitForRecovery(index)).thenThrow(new ElasticsearchException("TEST"));

        indexRangeService.calculateRange(index);
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest-EmptyCollection.json")
    public void testCalculateRangeWithEmptyIndex() throws Exception {
        final String index = "graylog";
        when(indices.indexRangeStatsOfIndex(index)).thenReturn(IndexRangeStats.EMPTY);
        when(indices.waitForRecovery(index)).thenReturn(HealthStatus.Green);

        final IndexRange range = indexRangeService.calculateRange(index);

        assertThat(range).isNotNull();
        assertThat(range.indexName()).isEqualTo(index);
        assertThat(range.begin()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(range.end()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
    }

    @Test
    public void testCalculateRangeWithNonExistingIndex() throws Exception {
        when(indices.waitForRecovery("does-not-exist")).thenReturn(HealthStatus.Red);

        assertThatThrownBy(() -> indexRangeService.calculateRange("does-not-exist"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unable to calculate range for index <does-not-exist>, index is unhealthy: Red");
    }

    @Test
    public void savePersistsIndexRange() throws Exception {
        final String indexName = "graylog";
        final DateTime begin = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final IndexRange indexRange = MongoIndexRange.create(indexName, begin, end, now, 42);

        indexRangeService.save(indexRange);

        final IndexRange result = indexRangeService.get(indexName);
        assertThat(result.indexName()).isEqualTo(indexName);
        assertThat(result.begin()).isEqualTo(begin);
        assertThat(result.end()).isEqualTo(end);
        assertThat(result.calculatedAt()).isEqualTo(now);
        assertThat(result.calculationDuration()).isEqualTo(42);
    }

    @Test
    public void saveOverwritesExistingIndexRange() throws Exception {
        final String indexName = "graylog";
        final DateTime begin = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final IndexRange indexRangeBefore = MongoIndexRange.create(indexName, begin, end, now, 1);
        final IndexRange indexRangeAfter = MongoIndexRange.create(indexName, begin, end, now, 2);

        indexRangeService.save(indexRangeBefore);

        final IndexRange before = indexRangeService.get(indexName);
        assertThat(before.calculationDuration()).isEqualTo(1);

        indexRangeService.save(indexRangeAfter);

        final IndexRange after = indexRangeService.get(indexName);
        assertThat(after.calculationDuration()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest.json")
    public void remove() throws Exception {
        assertThat(indexRangeService.findAll()).hasSize(2);

        assertThat(indexRangeService.remove("graylog_1")).isTrue();
        assertThat(indexRangeService.remove("graylog_1")).isFalse();

        assertThat(indexRangeService.findAll()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest.json")
    public void testHandleIndexDeletion() throws Exception {
        when(indexSetRegistry.isManagedIndex("graylog_1")).thenReturn(true);

        assertThat(indexRangeService.findAll()).hasSize(2);

        localEventBus.post(IndicesDeletedEvent.create(Collections.singleton("graylog_1")));

        assertThat(indexRangeService.findAll()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest.json")
    public void testHandleIndexClosing() throws Exception {
        when(indexSetRegistry.isManagedIndex("graylog_1")).thenReturn(true);

        assertThat(indexRangeService.findAll()).hasSize(2);

        localEventBus.post(IndicesClosedEvent.create(Collections.singleton("graylog_1")));

        assertThat(indexRangeService.findAll()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest.json")
    public void testHandleIndexReopening() throws Exception {
        final DateTime begin = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2016, 1, 15, 0, 0, DateTimeZone.UTC);
        when(indices.indexRangeStatsOfIndex("graylog_3")).thenReturn(IndexRangeStats.create(begin, end));
        when(indexSetRegistry.isManagedIndex("graylog_3")).thenReturn(true);
        when(indices.waitForRecovery("graylog_3")).thenReturn(HealthStatus.Green);

        localEventBus.post(IndicesReopenedEvent.create(Collections.singleton("graylog_3")));

        final SortedSet<IndexRange> indexRanges = indexRangeService.find(begin, end);
        assertThat(indexRanges).hasSize(1);
        assertThat(indexRanges.first().indexName()).isEqualTo("graylog_3");
        assertThat(indexRanges.first().begin()).isEqualTo(begin);
        assertThat(indexRanges.first().end()).isEqualTo(end);
    }

    @Test
    @MongoDBFixtures("MongoIndexRangeServiceTest.json")
    public void testHandleIndexReopeningWhenNotManaged() throws Exception {
        final DateTime begin = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2016, 1, 15, 0, 0, DateTimeZone.UTC);
        when(indexSetRegistry.isManagedIndex("graylog_3")).thenReturn(false);
        when(indices.indexRangeStatsOfIndex("graylog_3")).thenReturn(IndexRangeStats.EMPTY);

        localEventBus.post(IndicesReopenedEvent.create(Collections.singleton("graylog_3")));

        final SortedSet<IndexRange> indexRanges = indexRangeService.find(begin, end);
        assertThat(indexRanges).isEmpty();
    }
}
